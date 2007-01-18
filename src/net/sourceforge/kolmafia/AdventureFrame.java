/**
 * Copyright (c) 2005-2006, KoLmafia development team
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
 *  [3] Neither the name "KoLmafia" nor the names of its contributors may
 *      be used to endorse or promote products derived from this software
 *      without specific prior written permission.
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.BufferedReader;
import java.io.File;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.filechooser.FileFilter;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;

public class AdventureFrame extends KoLFrame
{
	private static JComboBox zoneSelect = null;
	private static JList locationSelect = null;
	private static JProgressBar requestMeter = null;
	private static KoLAdventure lastAdventure = null;

	private JTree combatTree;
	private JTextArea combatEditor;
	private DefaultTreeModel combatModel;
	private CardLayout combatCards;
	private JPanel combatPanel;

	private JComboBox dropdown1, dropdown2;
	private AdventureSelectPanel adventureSelect;

	private JComboBox hpAutoRecoverSelect, hpAutoRecoverTargetSelect, hpHaltCombatSelect;
	private JCheckBox [] hpRestoreCheckbox;
	private JComboBox mpAutoRecoverSelect, mpAutoRecoverTargetSelect, mpBalanceSelect;
	private JCheckBox [] mpRestoreCheckbox;

	/**
	 * Constructs a new <code>AdventureFrame</code>.  All constructed panels
	 * are placed into their corresponding tabs, with the content panel being
	 * defaulted to the adventure selection panel.
	 */

	public AdventureFrame()
	{
		super( "Adventure" );

		// Construct the adventure select container
		// to hold everything related to adventuring.

		this.adventureSelect = new AdventureSelectPanel();

		JPanel adventureDetails = new JPanel( new BorderLayout( 20, 20 ) );
		adventureDetails.add( adventureSelect, BorderLayout.CENTER );

		requestMeter = new JProgressBar();
		requestMeter.setOpaque( true );
		requestMeter.setStringPainted( true );
		requestMeter.setString( " " );

		JPanel meterPanel = new JPanel( new BorderLayout( 10, 10 ) );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.WEST );
		meterPanel.add( requestMeter, BorderLayout.CENTER );
		meterPanel.add( Box.createHorizontalStrut( 20 ), BorderLayout.EAST );

		adventureDetails.add( meterPanel, BorderLayout.SOUTH );

		framePanel.setLayout( new BorderLayout( 20, 20 ) );
		framePanel.add( adventureDetails, BorderLayout.NORTH );
		framePanel.add( getSouthernTabs(), BorderLayout.CENTER );

		updateSelectedAdventure( AdventureDatabase.getAdventure( StaticEntity.getProperty( "lastAdventure" ) ) );
		JComponentUtilities.setComponentSize( framePanel, 640, 480 );
	}

	public static void updateRequestMeter( String message )
	{
		if ( requestMeter == null )
			return;

		requestMeter.setString( message );
	}

	public static void updateRequestMeter( int value, int maximum )
	{
		if ( requestMeter == null )
			return;

		requestMeter.setMaximum( maximum );
		requestMeter.setValue( value );
	}

	public static void updateSelectedAdventure( KoLAdventure location )
	{
		if ( location == null || zoneSelect == null || locationSelect == null )
			return;

		zoneSelect.setSelectedItem( AdventureDatabase.ZONE_DESCRIPTIONS.get( location.getParentZone() ) );
		locationSelect.setSelectedValue( location, true );
	}

	public boolean useSidePane()
	{	return true;
	}

	private JPanel constructLabelPair( String label, JComponent element )
	{
		JPanel container = new JPanel( new BorderLayout() );

		if ( element instanceof JComboBox )
			JComponentUtilities.setComponentSize( element, 240, 20 );

		container.add( new JLabel( "<html><b>" + label + "</b></html>", JLabel.LEFT ), BorderLayout.NORTH );
		container.add( element, BorderLayout.CENTER );
		return container;
	}

	private JTabbedPane getSouthernTabs()
	{
		tabs = new JTabbedPane();
		tabs.setTabLayoutPolicy( JTabbedPane.SCROLL_TAB_LAYOUT );

		// Handle everything that might appear inside of the
		// session tally.

		JSplitPane sessionGrid = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, true,
			getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown1" ) ),
			getAdventureSummary( StaticEntity.getIntegerProperty( "defaultDropdown2" ) ) );

		sessionGrid.setDividerLocation( 0.5 );
		sessionGrid.setResizeWeight( 0.5 );
		tabs.addTab( "Normal Options", sessionGrid );

		// Components of auto-restoration

		JPanel restorePanel = new JPanel( new GridLayout( 1, 2, 10, 10 ) );

		JPanel healthPanel = new JPanel();
		healthPanel.add( new HealthOptionsPanel() );

		JPanel manaPanel = new JPanel();
		manaPanel.add( new ManaOptionsPanel() );

		restorePanel.add( healthPanel );
		restorePanel.add( manaPanel );

		CheckboxListener listener = new CheckboxListener();
		for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
			hpRestoreCheckbox[i].addActionListener( listener );
		for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
			mpRestoreCheckbox[i].addActionListener( listener );

		// Components of custom combat and choice adventuring,
		// combined into one friendly panel.

		combatTree = new JTree();
		combatModel = (DefaultTreeModel) combatTree.getModel();

		combatCards = new CardLayout();
		combatPanel = new JPanel( combatCards );
		combatPanel.add( "tree", new CustomCombatTreePanel() );
		combatPanel.add( "editor", new CustomCombatPanel() );

		addTab( "Choice Adventures", new ChoiceOptionsPanel() );
		tabs.addTab( "Combat Adventures", combatPanel );
		addTab( "Auto Recovery", restorePanel );

		return tabs;
	}

	private JPanel getAdventureSummary( int selectedIndex )
	{
		CardLayout resultCards = new CardLayout();
		JPanel resultPanel = new JPanel( resultCards );
		JComboBox resultSelect = new JComboBox();

		resultSelect.addItem( "Session Results" );
		resultPanel.add( new AdventureResultsPanel( tally ), "0" );

		resultSelect.addItem( "Location Details" );
		resultPanel.add( new SafetyField(), "1" );

		resultSelect.addItem( "Mood Summary" );
		resultPanel.add( new AdventureResultsPanel( MoodSettings.getTriggers() ), "2" );

		resultSelect.addItem( "Conditions Left" );
		resultPanel.add( new AdventureResultsPanel( conditions ), "3" );

		resultSelect.addItem( "Active Effects" );
		resultPanel.add( new AdventureResultsPanel( activeEffects ), "4" );

		resultSelect.addItem( "Visited Locations" );
		resultPanel.add( new AdventureResultsPanel( adventureList ), "5" );

		resultSelect.addItem( "Encounter Listing" );
		resultPanel.add( new AdventureResultsPanel( encounterList ), "6" );

		resultSelect.addActionListener( new ResultSelectListener( resultCards, resultPanel, resultSelect ) );

		JPanel containerPanel = new JPanel( new BorderLayout() );
		containerPanel.add( resultSelect, BorderLayout.NORTH );
		containerPanel.add( resultPanel, BorderLayout.CENTER );

		if ( dropdown1 == null )
		{
			dropdown1 = resultSelect;
			dropdown1.setSelectedIndex( selectedIndex );
		}
		else
		{
			dropdown2 = resultSelect;
			dropdown2.setSelectedIndex( selectedIndex );
		}

		return containerPanel;
	}

	public void requestFocus()
	{
		super.requestFocus();
		locationSelect.requestFocus();
	}

	private class ResultSelectListener implements ActionListener
	{
		private CardLayout resultCards;
		private JPanel resultPanel;
		private JComboBox resultSelect;

		public ResultSelectListener( CardLayout resultCards, JPanel resultPanel, JComboBox resultSelect )
		{
			this.resultCards = resultCards;
			this.resultPanel = resultPanel;
			this.resultSelect = resultSelect;
		}

		public void actionPerformed( ActionEvent e )
		{
			String index = String.valueOf( resultSelect.getSelectedIndex() );
			resultCards.show( resultPanel, index );
			StaticEntity.setProperty( resultSelect == dropdown1 ? "defaultDropdown1" : "defaultDropdown2", index );

		}
	}

	private class SafetyField extends JPanel implements Runnable, ListSelectionListener
	{
		private JLabel safetyText = new JLabel( " " );
		private String savedText = " ";

		public SafetyField()
		{
			super( new BorderLayout() );
			safetyText.setVerticalAlignment( JLabel.TOP );

			SimpleScrollPane textScroller = new SimpleScrollPane( safetyText, SimpleScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
			JComponentUtilities.setComponentSize( textScroller, 100, 100 );
			add( textScroller, BorderLayout.CENTER );

			KoLCharacter.addCharacterListener( new KoLCharacterAdapter( this ) );
			locationSelect.addListSelectionListener( this );

			setSafetyString();
		}

		public void run()
		{	setSafetyString();
		}

		public void valueChanged( ListSelectionEvent e )
		{	setSafetyString();
		}

		private void setSafetyString()
		{
			KoLAdventure request = (KoLAdventure) locationSelect.getSelectedValue();
			if ( request == null )
				return;

			AreaCombatData combat = request.getAreaSummary();
			String text = ( combat == null ) ? " " : combat.toString();

			// Avoid rendering and screen flicker if no change.
			// Compare with our own copy of what we set, since
			// getText() returns a modified version.

			if ( !text.equals( savedText ) )
			{
				savedText = text;
				safetyText.setText( text );
			}
		}
	}

	/**
	 * An internal class which represents the panel used for adventure
	 * selection in the <code>AdventureFrame</code>.
	 */

	private class AdventureSelectPanel extends JPanel
	{
		private boolean isHandlingConditions = false;

		private JComboBox moodSelect;
		private JComboBox actionSelect;
		private TreeMap zoneMap;
		private JSpinner countField;
		private JTextField conditionField;

		public AdventureSelectPanel()
		{
			super( new BorderLayout( 10, 10 ) );

			LockableListModel adventureList = AdventureDatabase.getAsLockableListModel();

			// West pane is a scroll pane which lists all of the available
			// locations -- to be included is a map on a separate tab.

			Object currentZone;
			zoneMap = new TreeMap();

			zoneSelect = new JComboBox();
			zoneSelect.addItem( "All Locations" );

			Object [] zones = AdventureDatabase.PARENT_LIST.toArray();

			for ( int i = 0; i < zones.length; ++i )
			{
				currentZone = AdventureDatabase.ZONE_DESCRIPTIONS.get( zones[i] );
				zoneMap.put( currentZone, zones[i] );
				zoneSelect.addItem( currentZone );
			}

			countField = new JSpinner();

			JComponentUtilities.setComponentSize( countField, 50, 24 );
			JComponentUtilities.setComponentSize( zoneSelect, 200, 24 );

			JPanel zonePanel = new JPanel( new BorderLayout( 5, 5 ) );
			zonePanel.add( countField, BorderLayout.EAST );
			zonePanel.add( zoneSelect, BorderLayout.CENTER );

			zoneSelect.addActionListener( new ZoneChangeListener() );
			locationSelect = new JList( adventureList );
			locationSelect.setVisibleRowCount( 4 );

			JPanel locationPanel = new JPanel( new BorderLayout( 5, 5 ) );
			locationPanel.add( zonePanel, BorderLayout.NORTH );
			locationPanel.add( new SimpleScrollPane( locationSelect ), BorderLayout.CENTER );

			JPanel westPanel = new JPanel( new CardLayout( 20, 20 ) );
			westPanel.add( locationPanel, "" );

			add( westPanel, BorderLayout.WEST );
			add( new ObjectivesPanel(), BorderLayout.CENTER );
		}

		private void handleConditions( KoLAdventure request )
		{
			String conditionList = conditionField.getText().trim().toLowerCase();

			if ( conditionList.equalsIgnoreCase( "none" ) )
			{
				conditions.clear();
				return;
			}

			if ( conditionList.length() == 0 )
				return;

			conditions.clear();
			boolean verifyConditions = conditionList.equalsIgnoreCase( AdventureDatabase.getCondition( request ) );

			int worthlessItemCount = 0;
			boolean useDisjunction = false;

			String [] splitConditions = conditionList.split( "\\s*,\\s*" );

			for ( int i = 0; i < splitConditions.length; ++i )
			{
				if ( splitConditions[i].indexOf( "worthless" ) != -1 )
				{
					// You're looking for some number of
					// worthless items

					worthlessItemCount += Character.isDigit( splitConditions[i].charAt(0) ) ?
						StaticEntity.parseInt( splitConditions[i].split( " " )[0] ) : 1;
				}
				if ( splitConditions[i].equals( "check" ) )
				{
					// Postpone verification of conditions
					// until all other conditions added.

					verifyConditions = true;
				}
				else if ( splitConditions[i].equals( "outfit" ) )
				{
					// Determine where you're adventuring and use
					// that to determine which components make up
					// the outfit pulled from that area.

					if ( !(request instanceof KoLAdventure) || !EquipmentDatabase.addOutfitConditions( (KoLAdventure) request ) )
						return;

					verifyConditions = true;
				}
				else if ( splitConditions[i].equals( "or" ) || splitConditions[i].equals( "and" ) || splitConditions[i].startsWith( "conjunction" ) || splitConditions[i].startsWith( "disjunction" ) )
				{
					useDisjunction = splitConditions[i].equals( "or" ) || splitConditions[i].startsWith( "disjunction" );
				}
				else
				{
					if ( !DEFAULT_SHELL.executeConditionsCommand( "add " + splitConditions[i] ) )
						return;
				}
			}

			if ( worthlessItemCount > 0 )
				StaticEntity.getClient().makeRequest( new WorthlessItemRequest( worthlessItemCount ) );

			if ( verifyConditions || worthlessItemCount > 0 )
			{
				KoLmafia.checkRequirements( conditions, false );
				if ( conditions.isEmpty() )
				{
					KoLmafia.updateDisplay( ABORT_STATE, "All conditions already satisfied." );
					return;
				}
			}

			if ( conditions.size() > 1 )
				DEFAULT_SHELL.executeConditionsCommand( useDisjunction ? "mode disjunction" : "mode conjunction" );

			if ( ((Integer)countField.getValue()).intValue() == 0 )
				countField.setValue( new Integer( KoLCharacter.getAdventuresLeft() ) );
		}

		private class ObjectivesPanel extends KoLPanel
		{
			private ExecuteButton begin;

			public ObjectivesPanel()
			{
				super( new Dimension( 80, 20 ), new Dimension( 100, 20 ) );

				actionSelect = new JComboBox( KoLCharacter.getBattleSkillNames() );
				moodSelect = new JComboBox( MoodSettings.getAvailableMoods() );

				conditionField = new JTextField();
				locationSelect.addListSelectionListener( new ConditionChangeListener() );

				JPanel buttonPanel = new JPanel();
				buttonPanel.add( begin = new ExecuteButton() );
				buttonPanel.add( new WorldPeaceButton() );

				JPanel buttonWrapper = new JPanel( new BorderLayout() );
				buttonWrapper.add( buttonPanel, BorderLayout.EAST );

				CombatActionChangeListener listener = new CombatActionChangeListener();
				actionSelect.addActionListener( listener );
				moodSelect.addActionListener( listener );

				VerifiableElement [] elements = new VerifiableElement[3];
				elements[0] = new VerifiableElement( "In Combat:  ", actionSelect );
				elements[1] = new VerifiableElement( "Use Mood:  ", moodSelect );
				elements[2] = new VerifiableElement( "Objectives:  ", conditionField );

				setContent( elements );
				container.add( buttonWrapper, BorderLayout.SOUTH );
			}

			public void actionConfirmed()
			{
			}

			public void actionCancelled()
			{
			}

			public boolean shouldAddStatusLabel( VerifiableElement [] elements )
			{	return false;
			}

			public void setEnabled( boolean isEnabled )
			{	begin.setEnabled( isEnabled );
			}
		}

		private class WorthlessItemRequest implements Runnable
		{
			private int itemCount;

			public WorthlessItemRequest( int itemCount )
			{	this.itemCount = itemCount;
			}

			public void run()
			{	DEFAULT_SHELL.executeLine( "acquire " + itemCount + " worthless item" );
			}
		}

		private class ZoneChangeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				if ( zoneSelect.getSelectedIndex() == 0 )
				{
					AdventureDatabase.refreshAdventureList();
					return;
				}

				String zone = (String) zoneSelect.getSelectedItem();

				if ( zone == null )
					return;

				zone = (String) zoneMap.get( zone );
				if ( zone == null )
					return;

				AdventureDatabase.refreshAdventureList( zone );
			}
		}

		private class ConditionChangeListener implements ListSelectionListener, ListDataListener
		{
			public ConditionChangeListener()
			{	conditions.addListDataListener( this );
			}

			public void valueChanged( ListSelectionEvent e )
			{
				if ( isHandlingConditions )
					return;

				fillCurrentConditions();
			}

			public void intervalAdded( ListDataEvent e )
			{
				if ( isHandlingConditions )
					return;

				fillCurrentConditions();
			}

			public void intervalRemoved( ListDataEvent e )
			{
				if ( isHandlingConditions )
					return;

				fillCurrentConditions();
			}

			public void contentsChanged( ListDataEvent e )
			{
				if ( isHandlingConditions )
					return;

				fillCurrentConditions();
			}

			public void fillDefaultConditions()
			{
				if ( !StaticEntity.getBooleanProperty( "autoSetConditions" ) )
					return;

				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
				if ( location == null )
					return;

				conditionField.setText( AdventureDatabase.getCondition( location ) );
			}

			public void fillCurrentConditions()
			{
				StringBuffer conditionString = new StringBuffer();

				for ( int i = 0; i < conditions.size(); ++i )
				{
					if ( i > 0 )
						conditionString.append( ", " );

					conditionString.append( ((AdventureResult)conditions.get(i)).toConditionString() );
				}

				if ( conditionString.length() == 0 )
					fillDefaultConditions();
				else
					conditionField.setText( conditionString.toString() );
			}
		}

		private class CombatActionChangeListener implements ActionListener
		{
			public void actionPerformed( ActionEvent e )
			{
				StaticEntity.setProperty( "battleAction", (String) actionSelect.getSelectedItem() );
				MoodSettings.setMood( (String) moodSelect.getSelectedItem() );
			}
		}

		private class ExecuteButton extends ActionButton
		{
			public ExecuteButton()
			{	super( "begin" );
			}

			public void actionPerformed( ActionEvent e )
			{
				if ( KoLmafia.isAdventuring() )
					return;

				KoLmafia.forceContinue();
				KoLmafia.updateDisplay( "Validating adventure sequence..." );

				KoLAdventure request = (KoLAdventure) locationSelect.getSelectedValue();
				if ( request == null )
				{
					KoLmafia.updateDisplay( ERROR_STATE, "No location selected." );
					return;
				}

				// If there are conditions in the condition field, be
				// sure to process them.

				if ( conditions.isEmpty() || (lastAdventure != null && lastAdventure != request) )
				{
					Object stats = null;
					int substatIndex = conditions.indexOf( tally.get(2) );

					if ( substatIndex != 0 )
						stats = conditions.get( substatIndex );

					conditions.clear();

					if ( stats != null )
						conditions.add( stats );

					lastAdventure = request;

					isHandlingConditions = true;

					RequestThread.openRequestSequence();
					handleConditions( request );
					RequestThread.closeRequestSequence();

					isHandlingConditions = false;

					if ( KoLmafia.refusesContinue() )
						return;
				}

				int requestCount = Math.min( getValue( countField, 1 ), KoLCharacter.getAdventuresLeft() );
				countField.setValue( new Integer( requestCount ) );

				StaticEntity.getClient().makeRequest( request, requestCount );
			}
		}

		private class WorldPeaceButton extends ActionButton
		{
			public WorldPeaceButton()
			{	super( "stop" );
			}

			public void actionPerformed( ActionEvent e )
			{	RequestThread.declareWorldPeace();
			}
		}

		public void requestFocus()
		{	locationSelect.requestFocus();
		}
	}

	/**
	 * An internal class which represents the panel used for tallying the
	 * results in the <code>AdventureFrame</code>.  Note that all of the
	 * tallying functionality is handled by the <code>LockableListModel</code>
	 * provided, so this functions as a container for that list model.
	 */

	private class AdventureResultsPanel extends JPanel
	{
		public AdventureResultsPanel( LockableListModel resultList )
		{	this( null, resultList, 11 );
		}

		public AdventureResultsPanel( String header, LockableListModel resultList, int rowCount )
		{
			setLayout( new BorderLayout() );

			ShowDescriptionList tallyDisplay = new ShowDescriptionList( resultList );
			tallyDisplay.setPrototypeCellValue( "ABCDEFGHIJKLMNOPQRSTUVWXYZ" );
			tallyDisplay.setVisibleRowCount( rowCount );

			add( new SimpleScrollPane( tallyDisplay ), BorderLayout.CENTER );

			if ( header != null )
				add( JComponentUtilities.createLabel( header, JLabel.CENTER, Color.black, Color.white ), BorderLayout.NORTH );
		}
	}

	private class CustomCombatPanel extends LabeledScrollPanel
	{
		public CustomCombatPanel()
		{
			super( "Editor", "save", "help", new JTextArea() );
			combatEditor = (JTextArea) scrollComponent;
			combatEditor.setFont( DEFAULT_FONT );
			refreshCombatSettings();
		}

		public void actionConfirmed()
		{
			File location = new File( SETTINGS_DIRECTORY, CombatSettings.settingsFileName() );
			if ( !location.exists() )
				CombatSettings.reset();

			LogStream writer = LogStream.openStream( location, true );
			writer.println( ((JTextArea)scrollComponent).getText() );
			writer.close();
			writer = null;

			KoLCharacter.battleSkillNames.setSelectedItem( "custom combat script" );
			StaticEntity.setProperty( "battleAction", "custom combat script" );

			// After storing all the data on disk, go ahead
			// and reload the data inside of the tree.

			refreshCombatTree();
			combatCards.show( combatPanel, "tree" );
		}

		public void actionCancelled()
		{	StaticEntity.openSystemBrowser( "http://kolmafia.sourceforge.net/combat.html" );
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private class CustomCombatTreePanel extends LabeledScrollPanel
	{
		public CustomCombatTreePanel()
		{	super( "Custom Combat", "edit", "load", combatTree );
		}

		public void actionConfirmed()
		{
			refreshCombatSettings();
			combatCards.show( combatPanel, "editor" );
		}

		public void actionCancelled()
		{
			JFileChooser chooser = new JFileChooser( (new File( "settings" )).getAbsolutePath() );
			chooser.setFileFilter( CCS_FILTER );

			int returnVal = chooser.showOpenDialog( null );

			if ( chooser.getSelectedFile() == null || returnVal != JFileChooser.APPROVE_OPTION )
				return;

			CombatSettings.loadSettings( chooser.getSelectedFile() );
			refreshCombatSettings();
		}

		public void setEnabled( boolean isEnabled )
		{
		}
	}

	private static final FileFilter CCS_FILTER = new FileFilter()
	{
		public boolean accept( File file )
		{
			String name = file.getName();
			return !name.startsWith( "." ) && name.startsWith( "combat_" );
		}

		public String getDescription()
		{	return "Custom Combat Settings";
		}
	};

	private void refreshCombatSettings()
	{
		if ( KoLCharacter.baseUserName().equals( "GLOBAL" ) )
			return;

		try
		{
			CombatSettings.reset();
			BufferedReader reader = KoLDatabase.getReader( "data" + File.separator + CombatSettings.settingsFileName() );

			StringBuffer buffer = new StringBuffer();
			String line;

			while ( (line = reader.readLine()) != null )
			{
				buffer.append( line );
				buffer.append( System.getProperty( "line.separator" ) );
			}

			reader.close();
			reader = null;

			// If the buffer is empty, add in the default settings.

			if ( buffer.length() == 0 )
				buffer.append( "[ default ]\n1: attack with weapon" );

			combatEditor.setText( buffer.toString() );
		}
		catch ( Exception e )
		{
			// This should not happen.  Therefore, print
			// a stack trace for debug purposes.

			StaticEntity.printStackTrace( e );
		}

		refreshCombatTree();
	}

	/**
	 * Internal class used to handle everything related to
	 * displaying custom combat.
	 */

	private void refreshCombatTree()
	{
		CombatSettings.reset();
		combatModel.setRoot( CombatSettings.getRoot() );
		combatTree.setRootVisible( false );

		for ( int i = 0; i < combatTree.getRowCount(); ++i )
			combatTree.expandRow( i );
	}

	/**
	 * This panel allows the user to select which item they would like
	 * to do for each of the different choice adventures.
	 */

	private class ChoiceOptionsPanel extends JPanel
	{
		private TreeMap choiceMap;
		private TreeMap selectMap;
		private CardLayout choiceCards;

		private JComboBox [] optionSelects;

		private JComboBox sewerSelect;
		private JComboBox castleWheelSelect;
		private JComboBox spookyForestSelect;
		private JComboBox violetFogSelect;
		private JComboBox maidenSelect;
		private JComboBox louvreSelect;
		private JComboBox billiardRoomSelect;
		private JComboBox riseSelect, fallSelect;

		/**
		 * Constructs a new <code>ChoiceOptionsPanel</code>.
		 */

		public ChoiceOptionsPanel()
		{
			choiceCards = new CardLayout( 10, 10 );

			choiceMap = new TreeMap();
			selectMap = new TreeMap();

			this.setLayout( choiceCards );
			add( new JPanel(), "" );

			String [] options;

			optionSelects = new JComboBox[ AdventureDatabase.CHOICE_ADVS.length ];
			for ( int i = 0; i < AdventureDatabase.CHOICE_ADVS.length; ++i )
			{
				optionSelects[i] = new JComboBox();
				options = AdventureDatabase.CHOICE_ADVS[i].getOptions();
				for ( int j = 0; j < options.length; ++j )
					optionSelects[i].addItem( options[j] );
			}

			sewerSelect = new JComboBox();
			options = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < options.length; ++i )
				sewerSelect.addItem( options[i] );

			castleWheelSelect = new JComboBox();
			castleWheelSelect.addItem( "Turn to map quest position (via moxie)" );
			castleWheelSelect.addItem( "Turn to map quest position (via mysticality)" );
			castleWheelSelect.addItem( "Turn to muscle position" );
			castleWheelSelect.addItem( "Turn to mysticality position" );
			castleWheelSelect.addItem( "Turn to moxie position" );
			castleWheelSelect.addItem( "Turn clockwise" );
			castleWheelSelect.addItem( "Turn counterclockwise" );
			castleWheelSelect.addItem( "Ignore this adventure" );

			spookyForestSelect = new JComboBox();
			spookyForestSelect.addItem( "Loot Seal Clubber corpse" );
			spookyForestSelect.addItem( "Loot Turtle Tamer corpse" );
			spookyForestSelect.addItem( "Loot Pastamancer corpse" );
			spookyForestSelect.addItem( "Loot Sauceror corpse" );
			spookyForestSelect.addItem( "Loot Disco Bandit corpse" );
			spookyForestSelect.addItem( "Loot Accordion Thief corpse" );

			violetFogSelect = new JComboBox();
			for ( int i = 0; i < VioletFog.FogGoals.length; ++i )
				violetFogSelect.addItem( VioletFog.FogGoals[i] );

			louvreSelect = new JComboBox();
			louvreSelect.addItem( "Ignore this adventure" );
			for ( int i = 0; i < Louvre.LouvreGoals.length - 3; ++i )
				louvreSelect.addItem( Louvre.LouvreGoals[i] );
			for ( int i = Louvre.LouvreGoals.length - 3; i < Louvre.LouvreGoals.length; ++i )
				louvreSelect.addItem( "Boost " + Louvre.LouvreGoals[i] );

			louvreSelect.addItem( "Boost Prime Stat" );
			louvreSelect.addItem( "Boost Lowest Stat" );

			maidenSelect = new JComboBox();
			maidenSelect.addItem( "Fight a random knight" );
			maidenSelect.addItem( "Only fight the wolf knight" );
			maidenSelect.addItem( "Only fight the snake knight" );
			maidenSelect.addItem( "Maidens, then fight a random knight" );
			maidenSelect.addItem( "Maidens, then fight the wolf knight" );
			maidenSelect.addItem( "Maidens, then fight the snake knight" );

			billiardRoomSelect = new JComboBox();
			billiardRoomSelect.addItem( "ignore this adventure" );
			billiardRoomSelect.addItem( "muscle substats" );
			billiardRoomSelect.addItem( "mysticality substats" );
			billiardRoomSelect.addItem( "moxie substats" );
			billiardRoomSelect.addItem( "Spookyraven Library Key" );

			riseSelect = new JComboBox();
			riseSelect.addItem( "ignore this adventure" );
			riseSelect.addItem( "boost mysticality substats" );
			riseSelect.addItem( "boost moxie substats" );
			riseSelect.addItem( "acquire mysticality skill" );
			riseSelect.addItem( "unlock second floor stairs" );

			fallSelect = new JComboBox();
			fallSelect.addItem( "ignore this adventure" );
			fallSelect.addItem( "boost muscle substats" );
			fallSelect.addItem( "reveal key in conservatory" );
			fallSelect.addItem( "unlock second floor stairs" );

			addChoiceSelect( "Beanstalk", "Castle Wheel", castleWheelSelect );
			addChoiceSelect( "Woods", "Forest Corpses", spookyForestSelect );
			addChoiceSelect( "Unsorted", "Violet Fog", violetFogSelect );
			addChoiceSelect( "Manor", "Billiard Room", billiardRoomSelect );
			addChoiceSelect( "Manor", "Rise of Spookyraven", riseSelect );
			addChoiceSelect( "Manor", "Fall of Spookyraven", fallSelect );
			addChoiceSelect( "Manor", "The Louvre", louvreSelect );
			addChoiceSelect( "Manor", "The Maidens", maidenSelect );

			addChoiceSelect( AdventureDatabase.LUCKY_SEWER.getZone(), AdventureDatabase.LUCKY_SEWER.getName(), sewerSelect );

			for ( int i = 0; i < optionSelects.length; ++i )
				addChoiceSelect( AdventureDatabase.CHOICE_ADVS[i].getZone(), AdventureDatabase.CHOICE_ADVS[i].getName(), optionSelects[i] );

			ArrayList optionsList;
			Object [] keys = choiceMap.keySet().toArray();

			for ( int i = 0; i < keys.length; ++i )
			{
				optionsList = (ArrayList) choiceMap.get( keys[i] );
				add( new ChoicePanel( optionsList ), (String) keys[i] );
			}

			actionCancelled();
			locationSelect.addListSelectionListener( new UpdateChoicesListener() );
		}

		private void addChoiceSelect( String zone, String name, JComboBox option )
		{
			if ( !choiceMap.containsKey( zone ) )
				choiceMap.put( zone, new ArrayList() );

			ArrayList options = (ArrayList) choiceMap.get( zone );

			if ( !options.contains( name ) )
			{
				options.add( name );
				selectMap.put( name, new ArrayList() );
			}

			options = (ArrayList) selectMap.get( name );
			options.add( option );
		}

		private class ChoicePanel extends KoLPanel
		{
			public ChoicePanel( ArrayList options )
			{
				super( new Dimension( 150, 20 ), new Dimension( 300, 20 ) );

				Object key;
				ArrayList value;

				ArrayList elementList = new ArrayList();

				for ( int i = 0; i < options.size(); ++i )
				{
					key = options.get(i);
					value = (ArrayList) selectMap.get( key );

					if ( value.size() == 1 )
					{
						elementList.add( new VerifiableElement( key + ":  ", (JComboBox) value.get(0) ) );
					}
					else
					{
						for ( int j = 0; j < value.size(); ++j )
							elementList.add( new VerifiableElement( key + " " + (j+1) + ":  ", (JComboBox) value.get(j) ) );
					}
				}

				VerifiableElement [] elements = new VerifiableElement[ elementList.size() ];
				elementList.toArray( elements );
				setContent( elements );
			}

			public void actionConfirmed()
			{	ChoiceOptionsPanel.this.actionConfirmed();
			}

			public void actionCancelled()
			{
			}

			public boolean shouldAddStatusLabel( VerifiableElement [] elements )
			{	return false;
			}

			public void setEnabled( boolean isEnabled )
			{
			}
		}

		private class UpdateChoicesListener implements ListSelectionListener
		{
			public void valueChanged( ListSelectionEvent e )
			{
				KoLAdventure location = (KoLAdventure) locationSelect.getSelectedValue();
				if ( location == null )
					return;

				choiceCards.show( ChoiceOptionsPanel.this, choiceMap.containsKey( location.getParentZone() ) ? location.getParentZone() : "" );
			}
		}

		public void actionConfirmed()
		{
			StaticEntity.setProperty( "violetFogGoal", String.valueOf( violetFogSelect.getSelectedIndex() ) );
			StaticEntity.setProperty( "luckySewerAdventure", (String) sewerSelect.getSelectedItem() );
			StaticEntity.setProperty( "choiceAdventure89", String.valueOf( maidenSelect.getSelectedIndex() ) );

			int louvreGoal = louvreSelect.getSelectedIndex();
			StaticEntity.setProperty( "choiceAdventure91",  String.valueOf( louvreGoal > 0 ? "1" : "2" ) );
			StaticEntity.setProperty( "louvreDesiredGoal", String.valueOf( louvreGoal ) );

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				int index = optionSelects[i].getSelectedIndex();
				String choice = AdventureDatabase.CHOICE_ADVS[i].getSetting();
				StaticEntity.setProperty( choice, String.valueOf( index + 1 ) );
			}

			//              The Wheel:

			//              Muscle
			// Moxie          +         Mysticality
			//            Map Quest

			// Option 1: Turn the wheel clockwise
			// Option 2: Turn the wheel counterclockwise
			// Option 3: Leave the wheel alone

			switch ( castleWheelSelect.getSelectedIndex() )
			{
			case 0: // Map quest position (choice adventure 11)
									// Muscle goes through moxie
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 1: // Map quest position (choice adventure 11)
									// Muscle goes through mysticality
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 2: // Muscle position (choice adventure 9)
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 3: // Mysticality position (choice adventure 10)
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 4: // Moxie position (choice adventure 12)
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;

			case 5: // Turn the wheel clockwise
				StaticEntity.setProperty( "choiceAdventure9", "1" );	  // Turn the muscle position clockwise
				StaticEntity.setProperty( "choiceAdventure10", "1" );  // Turn the mysticality position clockwise
				StaticEntity.setProperty( "choiceAdventure11", "1" );  // Turn the map quest position clockwise
				StaticEntity.setProperty( "choiceAdventure12", "1" );  // Turn the moxie position clockwise
				break;

			case 6: // Turn the wheel counterclockwise
				StaticEntity.setProperty( "choiceAdventure9", "2" );	  // Turn the muscle position counterclockwise
				StaticEntity.setProperty( "choiceAdventure10", "2" );  // Turn the mysticality position counterclockwise
				StaticEntity.setProperty( "choiceAdventure11", "2" );  // Turn the map quest position counterclockwise
				StaticEntity.setProperty( "choiceAdventure12", "2" );  // Turn the moxie position counterclockwise
				break;

			case 7: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure9", "3" );	  // Leave the muscle position alone
				StaticEntity.setProperty( "choiceAdventure10", "3" );  // Leave the mysticality position alone
				StaticEntity.setProperty( "choiceAdventure11", "3" );  // Leave the map quest position alone
				StaticEntity.setProperty( "choiceAdventure12", "3" );  // Leave the moxie position alone
				break;
			}

			switch ( spookyForestSelect.getSelectedIndex() )
			{
			case 0: // Seal clubber corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "1" );
				break;

			case 1: // Turtle tamer corpse
				StaticEntity.setProperty( "choiceAdventure26", "1" );
				StaticEntity.setProperty( "choiceAdventure27", "2" );
				break;

			case 2: // Pastamancer corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "1" );
				break;

			case 3: // Sauceror corpse
				StaticEntity.setProperty( "choiceAdventure26", "2" );
				StaticEntity.setProperty( "choiceAdventure28", "2" );
				break;

			case 4: // Disco bandit corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "1" );
				break;

			case 5: // Accordion thief corpse
				StaticEntity.setProperty( "choiceAdventure26", "3" );
				StaticEntity.setProperty( "choiceAdventure29", "2" );
				break;
			}

			switch ( billiardRoomSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure77", "3" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "2" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 2: // Mysticality
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "2" );
				break;

			case 3: // Moxie
				StaticEntity.setProperty( "choiceAdventure77", "1" );
				StaticEntity.setProperty( "choiceAdventure78", "3" );
				StaticEntity.setProperty( "choiceAdventure79", "3" );
				break;

			case 4: // Library Key
				StaticEntity.setProperty( "choiceAdventure77", "2" );
				StaticEntity.setProperty( "choiceAdventure78", "1" );
				StaticEntity.setProperty( "choiceAdventure79", "1" );
				break;
			}

			switch ( riseSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure80", "4" );

			case 1: // Mysticality
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "1" );
				break;

			case 2: // Moxie
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "2" );
				break;

			case 3: // Mysticality Class Skill
				StaticEntity.setProperty( "choiceAdventure80", "3" );
				StaticEntity.setProperty( "choiceAdventure88", "3" );
				break;

			case 4: // Second Floor
				StaticEntity.setProperty( "choiceAdventure80", "99" );
				break;
			}

			switch ( fallSelect.getSelectedIndex() )
			{
			case 0: // Ignore this adventure
				StaticEntity.setProperty( "choiceAdventure81", "4" );
				break;

			case 1: // Muscle
				StaticEntity.setProperty( "choiceAdventure81", "3" );

			case 2: // Gallery Key
				StaticEntity.setProperty( "choiceAdventure81", "1" );
				StaticEntity.setProperty( "choiceAdventure87", "2" );
				break;

			case 3: // Second Floor
				StaticEntity.setProperty( "choiceAdventure81", "99" );
				break;
			}
		}

		public void actionCancelled()
		{
			int index = StaticEntity.getIntegerProperty( "violetFogGoal" );
			if ( index >= 0 )
				violetFogSelect.setSelectedIndex( index );

			index = StaticEntity.getIntegerProperty( "louvreDesiredGoal" );
			if ( index >= 0 )
				louvreSelect.setSelectedIndex( index );

			maidenSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure89" ) );

			boolean foundItem = false;
			String sewerItem = StaticEntity.getProperty( "luckySewerAdventure" );

			String [] sewerOptions = AdventureDatabase.LUCKY_SEWER.getOptions();
			for ( int i = 0; i < sewerOptions.length; ++i )
			{
				if ( sewerOptions[i].equals( sewerItem ) )
				{
					foundItem = true;
					sewerSelect.setSelectedItem( sewerItem );
				}
			}

			if ( !foundItem )
			{
				StaticEntity.setProperty( "luckySewerAdventure", "stolen accordion" );
				sewerSelect.setSelectedItem( "stolen accordion" );
			}

			for ( int i = 1; i < optionSelects.length; ++i )
			{
				index = StaticEntity.getIntegerProperty( AdventureDatabase.CHOICE_ADVS[i].getSetting() );
				if ( index > 0 )
					optionSelects[i].setSelectedIndex( index - 1 );
			}

			// Determine the desired wheel position by examining
			// which choice adventure has the "3" value.
			// If none are "3", may be clockwise or counterclockwise
			// If they are all "3", leave wheel alone

			int [] counts = { 0, 0, 0, 0 };
			int option3 = 11;

			for ( int i = 9; i < 13; ++i )
			{
				int choice = StaticEntity.getIntegerProperty( "choiceAdventure" + i );
				counts[choice]++;

				if ( choice == 3 )
					option3 = i;
			}

			index = 0;

			if ( counts[1] == 4 )
			{
				// All choices say turn clockwise
				index = 5;
			}
			else if ( counts[2] == 4 )
			{
				// All choices say turn counterclockwise
				index = 6;
			}
			else if ( counts[3] == 4 )
			{
				// All choices say leave alone
				index = 7;
			}
			else if ( counts[3] != 1 )
			{
				// Bogus. Assume map quest
				index = 0;
			}
			else if ( option3 == 9)
			{
				// Muscle says leave alone
				index = 2;
			}
			else if ( option3 == 10)
			{
				// Mysticality says leave alone
				index = 3;
			}
			else if ( option3 == 11 )
			{
				// Map Quest says leave alone. If we turn
				// clockwise twice, we are going through
				// mysticality. Otherwise, through moxie.
				index = ( counts[1] == 2 ) ? 1 : 0;
			}
			else if ( option3 == 12 )
			{
				// Moxie says leave alone
				index = 4;
			}

			if ( index >= 0 )
				castleWheelSelect.setSelectedIndex( index );

			// Now, determine what is located in choice adventure #26,
			// which shows you which slot (in general) to use.

			index = StaticEntity.getIntegerProperty( "choiceAdventure26" );
			index = index * 2 + StaticEntity.getIntegerProperty( "choiceAdventure" + (26 + index) ) - 3;

			spookyForestSelect.setSelectedIndex( index < 0 ? 5 : index );

			// Figure out what to do in the billiard room

			switch ( StaticEntity.getIntegerProperty( "choiceAdventure77" ) )
			{
			case 1:

				// Moxie
				index = 3;
				break;

			case 2:
				index = StaticEntity.getIntegerProperty( "choiceAdventure78" );

				switch ( index )
				{
				case 1:
					index = StaticEntity.getIntegerProperty( "choiceAdventure79" );
					index = index == 1 ? 4 : index == 2 ? 2 : 0;
					break;
				case 2:
					// Muscle
					index = 1;
					break;
				case 3:
					// Ignore this adventure
					index = 0;
					break;
				}

				break;

			case 3:

				// Ignore this adventure
				index = 0;
				break;
			}

			if ( index >= 0 )
				billiardRoomSelect.setSelectedIndex( index );

			// Figure out what to do at the bookcases

			index = StaticEntity.getIntegerProperty( "choiceAdventure80" );
			if ( index == 4 )
				riseSelect.setSelectedIndex(0);
			else if ( index == 99 )
				riseSelect.setSelectedIndex(4);
			else
				riseSelect.setSelectedIndex( StaticEntity.getIntegerProperty( "choiceAdventure88" ) );

			index = StaticEntity.getIntegerProperty( "choiceAdventure81" );
			if ( index == 4 )
				fallSelect.setSelectedIndex(0);
			else if ( index == 3 )
				fallSelect.setSelectedIndex(1);
			else if ( index == 99 )
				riseSelect.setSelectedIndex(3);
			else
				fallSelect.setSelectedIndex(2);
		}
	}

	private void saveRestoreSettings()
	{
		StaticEntity.setProperty( "hpThreshold", String.valueOf( ((float)(hpHaltCombatSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecovery", String.valueOf( ((float)(hpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryTarget", String.valueOf( ((float)(hpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "hpAutoRecoveryItems", getSettingString( hpRestoreCheckbox ) );

		StaticEntity.setProperty( "mpThreshold", String.valueOf( ((float)(mpBalanceSelect.getSelectedIndex()) / 10.0f) ) );
		StaticEntity.setProperty( "mpAutoRecovery", String.valueOf( ((float)(mpAutoRecoverSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "mpAutoRecoveryTarget", String.valueOf( ((float)(mpAutoRecoverTargetSelect.getSelectedIndex() - 1) / 10.0f) ) );
		StaticEntity.setProperty( "mpAutoRecoveryItems", getSettingString( mpRestoreCheckbox ) );
	}

	private class CheckboxListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	private class HealthOptionsPanel extends JPanel implements ActionListener
	{
		private boolean refreshSoon = false;

		public HealthOptionsPanel()
		{
			hpHaltCombatSelect = new JComboBox();
			hpHaltCombatSelect.addItem( "Stop automation if auto-recovery fails" );
			for ( int i = 0; i <= 10; ++i )
				hpHaltCombatSelect.addItem( "Stop automation if health at " + (i*10) + "%" );

			hpAutoRecoverSelect = new JComboBox();
			hpAutoRecoverSelect.addItem( "Do not autorecover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverSelect.addItem( "Auto-recover health at " + (i*10) + "%" );

			hpAutoRecoverTargetSelect = new JComboBox();
			hpAutoRecoverTargetSelect.addItem( "Do not automatically recover health" );
			for ( int i = 0; i <= 10; ++i )
				hpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% health" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( constructLabelPair( "Stop automation: ", hpHaltCombatSelect ) );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Restore your health: ", hpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( hpAutoRecoverTargetSelect, 240, 20 );
			add( hpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Use these restores: ", constructScroller( hpRestoreCheckbox = HPRestoreItemList.getCheckboxes() ) ) );

			hpHaltCombatSelect.setSelectedIndex( Math.max( (int)(StaticEntity.getFloatProperty( "hpThreshold" ) * 10) + 1, 0 ) );
			hpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecovery" ) * 10) + 1 );
			hpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "hpAutoRecoveryTarget" ) * 10) + 1 );

			hpHaltCombatSelect.addActionListener( this );
			hpAutoRecoverSelect.addActionListener( this );
			hpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < hpRestoreCheckbox.length; ++i )
				hpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}

	private class ManaOptionsPanel extends JPanel implements ActionListener
	{
		public ManaOptionsPanel()
		{
			mpBalanceSelect = new JComboBox();
			mpBalanceSelect.addItem( "Only recast buffs for mood swings" );
			for ( int i = 1; i <= 9; ++i )
				mpBalanceSelect.addItem( "Enable conditional recast at " + (i*10) + "%" );

			mpAutoRecoverSelect = new JComboBox();
			mpAutoRecoverSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverSelect.addItem( "Auto-recover mana at " + (i*10) + "%" );

			mpAutoRecoverTargetSelect = new JComboBox();
			mpAutoRecoverTargetSelect.addItem( "Do not automatically recover mana" );
			for ( int i = 0; i <= 10; ++i )
				mpAutoRecoverTargetSelect.addItem( "Try to recover up to " + (i*10) + "% mana" );

			// Add the elements to the panel

			setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
			add( constructLabelPair( "Buff balancing: ", mpBalanceSelect ) );
			add( Box.createVerticalStrut( 10 ) );

			add( constructLabelPair( "Restore your mana: ", mpAutoRecoverSelect ) );
			add( Box.createVerticalStrut( 5 ) );

			JComponentUtilities.setComponentSize( mpAutoRecoverTargetSelect, 240, 20 );
			add( mpAutoRecoverTargetSelect );
			add( Box.createVerticalStrut( 10 ) );
			add( constructLabelPair( "Use these restores: ", constructScroller( mpRestoreCheckbox = MPRestoreItemList.getCheckboxes() ) ) );

			mpBalanceSelect.setSelectedIndex( Math.max( (int)(StaticEntity.getFloatProperty( "mpThreshold" ) * 10), 0 ) );
			mpAutoRecoverSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecovery" ) * 10) + 1 );
			mpAutoRecoverTargetSelect.setSelectedIndex( (int)(StaticEntity.getFloatProperty( "mpAutoRecoveryTarget" ) * 10) + 1 );

			mpBalanceSelect.addActionListener( this );
			mpAutoRecoverSelect.addActionListener( this );
			mpAutoRecoverTargetSelect.addActionListener( this );

			for ( int i = 0; i < mpRestoreCheckbox.length; ++i )
				mpRestoreCheckbox[i].addActionListener( this );
		}

		public void actionPerformed( ActionEvent e )
		{	saveRestoreSettings();
		}
	}
}
