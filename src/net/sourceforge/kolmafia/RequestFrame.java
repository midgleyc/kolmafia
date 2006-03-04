/**
 * Copyright (c) 2005, KoLmafia development team
 * http://kolmafia.sourceforge.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "KoLmafia development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.sourceforge.kolmafia;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.Box;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.ListSelectionModel;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.java.dev.spellcast.utilities.JComponentUtilities;

public class RequestFrame extends KoLFrame
{
	private int currentLocation = 0;
	private List visitedLocations = new ArrayList();

	private int combatRound;
	private String lastResponseText = "";

	private RequestFrame parent;
	private KoLRequest currentRequest;
	private LimitedSizeChatBuffer mainBuffer;

	private boolean hasSideBar;
	private boolean isRefreshing = false;
	private LimitedSizeChatBuffer sideBuffer;
	private CharpaneRequest sidePaneRequest;

	private JTextField locationField = new JTextField();
	protected JEditorPane sideDisplay;
	protected JEditorPane mainDisplay;

	public RequestFrame( KoLmafia client )
	{	this( client, new KoLRequest( client, "main.php" ) );
	}

	public RequestFrame( KoLmafia client, KoLRequest request )
	{	this( client, null, request );
	}

	public RequestFrame( KoLmafia client, RequestFrame parent, KoLRequest request )
	{
		super( client, "" );

		this.parent = parent;

		this.currentRequest = getClass() == RequestFrame.class && client != null && client.getCurrentRequest() instanceof FightRequest ?
			client.getCurrentRequest() : request;

		this.hasSideBar = getClass() == RequestFrame.class && request != null && !request.getURLString().startsWith( "desc" ) &&
			 !request.getURLString().startsWith( "doc" ) && !request.getURLString().startsWith( "searchp" );

		setCombatRound( request );

		this.mainDisplay = new JEditorPane();
		this.mainDisplay.setEditable( false );

		if ( !(this instanceof PendingTradesFrame) )
			this.mainDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

		this.mainBuffer = new LimitedSizeChatBuffer( "Mini-Browser", false );
		JScrollPane mainScroller = this.mainBuffer.setChatDisplay( this.mainDisplay );

		// Game text descriptions and player searches should not add
		// extra requests to the server by having a side panel.

		if ( !hasSideBar )
		{
			this.sideBuffer = null;

			JComponentUtilities.setComponentSize( mainScroller, 400, 300 );
			framePanel.setLayout( new BorderLayout() );
			framePanel.add( mainScroller, BorderLayout.CENTER );
		}
		else
		{
			this.sideDisplay = new JEditorPane();
			this.sideDisplay.setEditable( false );
			this.sideDisplay.addHyperlinkListener( new KoLHyperlinkAdapter() );

			this.sideBuffer = new LimitedSizeChatBuffer( "", false );
			JScrollPane sideScroller = this.sideBuffer.setChatDisplay( sideDisplay );
			JComponentUtilities.setComponentSize( sideScroller, 150, 450 );

			JSplitPane horizontalSplit = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true, sideScroller, mainScroller );
			horizontalSplit.setOneTouchExpandable( true );
			JComponentUtilities.setComponentSize( horizontalSplit, 600, 450 );

			// Add the standard locations handled within the
			// mini-browser, including inventory, character
			// information, skills and account setup.

			BrowserComboBox functionSelect = new BrowserComboBox();
			functionSelect.addItem( new BrowserComboBoxItem( " - Function - ", "" ) );

			functionSelect.addItem( new BrowserComboBoxItem( "Consumables", "inventory.php?which=1" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Equipment", "inventory.php?which=2" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Miscellaneous", "inventory.php?which=3" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Character Sheet", "charsheet.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Terrarium", "familiar.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Usable Skills", "skills.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Read Messages", "messages.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Quest Log", "questlog.php" ) );
			functionSelect.addItem( new BrowserComboBoxItem( "Account Menu", "account.php" ) );

			// Add the browser "goto" menu, because people
			// are familiar with seeing this as well.  But,
			// place it all inside of a "travel" menu.

			BrowserComboBox gotoSelect = new BrowserComboBox();
			gotoSelect.addItem( new BrowserComboBoxItem( " - Goto (Maki) - ", "" ) );

			gotoSelect.addItem( new BrowserComboBoxItem( "Main Map", "main.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Seaside Town", "town.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Clan Hall", "clan_hall.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Campground", "campground.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Big Mountains", "mountains.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Mt. McLargeHuge", "mclargehuge.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Nearby Plains", "plains.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Above Beanstalk", "beanstalk.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Sorceress' Lair", "lair.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Desert Beach", "beach.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Distant Woods", "woods.php" ) );
			gotoSelect.addItem( new BrowserComboBoxItem( "Mysterious Island", "island.php" ) );

			JPanel topMenu = new JPanel();
			topMenu.setOpaque( true );
			topMenu.setBackground( Color.white );

			topMenu.add( functionSelect );
			topMenu.add( gotoSelect );
			topMenu.add( Box.createHorizontalStrut( 20 ) );

			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" );
			RequestEditorKit.downloadImage( "http://images.kingdomofloathing.com/itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" );

			topMenu.add( new JLabel( JComponentUtilities.getSharedImage( "itemimages/smoon" + MoonPhaseDatabase.getRonaldPhase() + ".gif" ) ) );
			topMenu.add( new JLabel( JComponentUtilities.getSharedImage( "itemimages/smoon" + MoonPhaseDatabase.getGrimacePhase() + ".gif" ) ) );

			functionSelect.setSelectedIndex( 0 );
			gotoSelect.setSelectedIndex( 0 );

			framePanel.setLayout( new BorderLayout() );
			framePanel.add( topMenu, BorderLayout.NORTH );
			framePanel.add( horizontalSplit, BorderLayout.CENTER );

			// Add toolbar pieces so that people can quickly
			// go to locations they like.
		}

		toolbarPanel.add( new BackButton() );
		toolbarPanel.add( new ForwardButton() );
		toolbarPanel.add( new HomeButton() );
		toolbarPanel.add( new ReloadButton() );

		toolbarPanel.add( new JToolBar.Separator() );
		toolbarPanel.add( locationField );
		toolbarPanel.add( new JToolBar.Separator() );

		GoButton button = new GoButton();
		toolbarPanel.add( button );
		getRootPane().setDefaultButton( button );

		(new DisplayRequestThread( this.currentRequest )).start();
	}

	private class BrowserComboBox extends JComboBox implements ActionListener
	{
		public BrowserComboBox()
		{	addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			BrowserComboBox source = (BrowserComboBox) e.getSource();
			BrowserComboBoxItem selected = (BrowserComboBoxItem) source.getSelectedItem();

			if ( !selected.getLocation().equals( "" ) )
				refresh( new KoLRequest( client, selected.getLocation() ) );

			source.setSelectedIndex( 0 );
		}
	}

	private class BrowserComboBoxItem
	{
		private String name;
		private String location;

		public BrowserComboBoxItem( String name, String location )
		{
			this.name = name;
			this.location = location;
		}

		public String toString()
		{	return name;
		}

		public String getLocation()
		{	return location;
		}
	}

	/**
	 * Returns whether or not this request frame has a side bar.
	 * This is used to ensure that bookmarks correctly use a
	 * new frame if this frame does not have one.
	 */

	public boolean hasSideBar()
	{	return hasSideBar;
	}

	/**
	 * Utility method which returns the current URL being pointed
	 * to by this <code>RequestFrame</code>.
	 */

	public String getCurrentLocation()
	{	return currentRequest.getURLString();
	}

	/**
	 * Utility method which refreshes the current frame with
	 * data contained in the given request.  If the request
	 * has not yet been run, it will be run before the data
	 * is display in this frame.
	 */

	public void refresh( KoLRequest request )
	{
		String location = request.getURLString();

		if ( parent == null || location.startsWith( "search" ) || location.startsWith( "desc" ) )
		{
			setCombatRound( request );

			// Only record raw mini-browser requests
			if ( client != null && request.getClass() == KoLRequest.class )
				client.getMacroStream().println( location );

			(new DisplayRequestThread( request )).start();
		}
		else
			parent.refresh( request );
	}

	private void setCombatRound( KoLRequest request )
	{
		if ( request != null && request instanceof FightRequest )
			combatRound = ((FightRequest)request).getCombatRound();
		else
			combatRound = 1;
	}

	/**
	 * Utility method which refreshes the side pane.  This
	 * is used whenever something occurs in the main pane
	 * which is suspected to cause some display change here.
	 */

	private void refreshSidePane()
	{
		if ( !hasSideBar )
			return;

		if ( isRefreshing )
			return;

		isRefreshing = true;

		if ( sidePaneRequest == null )
			sidePaneRequest = new CharpaneRequest( client );

		sidePaneRequest.run();
		sideBuffer.clearBuffer();
		sideBuffer.append( getDisplayHTML( sidePaneRequest.responseText ) );
		sideDisplay.setCaretPosition(0);

		isRefreshing = false;
	}

	protected String getDisplayHTML( String responseText )
	{	return RequestEditorKit.getDisplayHTML( responseText );
	}

	/**
	 * A special thread class which ensures that attempts to
	 * refresh the frame with data do not long the Swing thread.
	 */

	protected class DisplayRequestThread extends DaemonThread
	{
		private KoLRequest request;

		public DisplayRequestThread( KoLRequest request )
		{	this.request = request;
		}

		public void run()
		{
			synchronized ( DisplayRequestThread.class )
			{
				if ( client == null || request == null || (request.responseText != null && !lastResponseText.equals( "" ) && request.responseText.equals( lastResponseText )) )
					return;

				mainBuffer.clearBuffer();
				mainBuffer.append( "Retrieving..." );

				if ( cloverCheckNeeded() )
				{
					if ( getProperty( "cloverProtectActive" ).equals( "true" ) )
					{
						client.updateDisplay( ERROR_STATE, "You have a ten-leaf clover." );

						mainBuffer.clearBuffer();
						mainBuffer.append( "<h1><font color=\"red\">You have a ten-leaf clover.	 Please deactivate clover protection in your startup options first if you are certain you want to use your clovers while adventuring.</font></h1>" );
						lastResponseText = "";
						return;
					}

					client.processResult( SewerRequest.CLOVER );
				}

				currentRequest = request;
				setupRequest();

				if ( request != null && request.responseText != null && request.responseText.length() != 0 )
				{
					client.setCurrentRequest( request );
					lastResponseText = request.responseText;
					displayRequest();
				}
				else
				{
					// If this resulted in a redirect, then update the display
					// to indicate that you were redirected and the display
					// cannot be shown in the minibrowser.

					mainBuffer.clearBuffer();
					mainBuffer.append( "Redirected to unknown page: &lt;" + request.redirectLocation + "&gt;" );
					lastResponseText = "";
					return;
				}
			}

			// Have the client update occur outside of the
			// synchronization block so that the appearance
			// of a GUI lockup doesn't happen.

			updateClient();
		}

		private boolean cloverCheckNeeded()
		{
			String adventure = request.getURLString();

			if ( getCurrentLocation().startsWith( "adventure.php" ) && adventure != null )
			{
				Matcher dataMatcher = Pattern.compile( "adv=(\\d+)" ).matcher( adventure );
				return client.isLuckyCharacter() && dataMatcher.find() && AdventureRequest.hasLuckyVersion( dataMatcher.group(1) );
			}

			return false;
		}

		private void setupRequest()
		{
			if ( request == null )
				return;

			// Update the title for the RequestFrame to include the
			// current round of combat (for people who track this
			// sort of thing).

			if ( getCurrentLocation().startsWith( "fight" ) )
				setTitle( "Mini-Browser: Combat Round " + combatRound );
			else
				setTitle( "Mini-Browser" );

			if ( request.responseText == null || request.responseText.length() == 0 )
			{
				// New prevention mechanism: tell the requests that there
				// will be no synchronization.

				String original = getProperty( "synchronizeFightFrame" );
				setProperty( "synchronizeFightFrame", "false" );
				request.run();
				setProperty( "synchronizeFightFrame", original );
			}
		}

		private void displayRequest()
		{
			mainBuffer.clearBuffer();

			// Function exactly like a history in a normal browser -
			// if you open a new frame after going back, all the ones
			// in the future get removed.

			while ( visitedLocations.size() > currentLocation )
				visitedLocations.remove( currentLocation );

			visitedLocations.add( request );
			locationField.setText( request.getURLString() );
			currentLocation = visitedLocations.size();

			mainBuffer.append( getDisplayHTML( lastResponseText ) );
			mainDisplay.setCaretPosition( 0 );
		}

		private void updateClient()
		{
			// In the event that something resembling a gain event
			// is seen in the response text, or in the event that you
			// switch between compact and full mode, refresh the sidebar.

			KoLCharacter.refreshCalculatedLists();
			String location = request.getURLString();

			if ( hasSideBar && (sidePaneRequest == null || location.indexOf( "?" ) != -1) )
				refreshSidePane();

			// Keep the client updated of your current equipment and
			// familiars, if you visit the appropriate pages.

			if ( location.startsWith( "inventory.php?which=2" ) )
				EquipmentRequest.parseEquipment( request.responseText );

			if ( location.startsWith( "familiar.php" ) )
				FamiliarData.registerFamiliarData( client, request.responseText );

			// See if the person learned a new skill from using a
			// mini-browser frame.

			Matcher learnedMatcher = Pattern.compile( "<td>You learn a new skill: <b>(.*?)</b>" ).matcher( request.responseText );
			if ( learnedMatcher.find() )
				KoLCharacter.addAvailableSkill( new UseSkillRequest( client, learnedMatcher.group(1), "", 1 ) );

			// Unfortunately, if you learn a new skill from Frank
			// the Regnaissance Gnome at the Gnomish Gnomads
			// Camp, it doesn't tell you the name of the skill.
			// It simply says: "You leargn a new skill. Whee!"

			if ( lastResponseText.indexOf( "You leargn a new skill." ) != -1 )
			     (new CharsheetRequest( client )).run();
		}
	}

	private class HomeButton extends JButton implements ActionListener
	{
		public HomeButton()
		{
			super( JComponentUtilities.getSharedImage( "home.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	refresh( new KoLRequest( client, "main.php" ) );
		}
	}

	private class BackButton extends JButton implements ActionListener
	{
		public BackButton()
		{
			super( JComponentUtilities.getSharedImage( "back.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( currentLocation > 1 )
			{
				--currentLocation;
				refresh( (KoLRequest) visitedLocations.get( currentLocation - 1 ) );
			}
		}
	}

	private class ForwardButton extends JButton implements ActionListener
	{
		public ForwardButton()
		{
			super( JComponentUtilities.getSharedImage( "forward.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			if ( currentLocation + 1 < visitedLocations.size() )
			{
				++currentLocation;
				refresh( (KoLRequest) visitedLocations.get( currentLocation - 1 ) );
			}
		}
	}

	private class ReloadButton extends JButton implements ActionListener
	{
		public ReloadButton()
		{
			super( JComponentUtilities.getSharedImage( "reload.gif" ) );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			visitedLocations.remove( currentRequest );
			refresh( extractRequest( currentRequest.getURLString() ) );
		}
	}

	private class GoButton extends JButton implements ActionListener
	{
		public GoButton()
		{
			super( "Go" );
			addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{
			KoLAdventure adventure = AdventureDatabase.getAdventure( locationField.getText() );
			KoLRequest request = extractRequest( adventure == null ? locationField.getText() : adventure.getRequest().getURLString() );
			client.getMacroStream().println( request.getURLString() );
			refresh( request );
		}
	}

	public void dispose()
	{
		visitedLocations.clear();
		super.dispose();
	}

	/**
	 * The main method used in the event of testing the way the
	 * user interface looks.  This allows the UI to be tested
	 * without having to constantly log in and out of KoL.
	 */

	public static void main( String [] args )
	{	(new CreateFrameRunnable( RequestFrame.class )).run();
	}
}
