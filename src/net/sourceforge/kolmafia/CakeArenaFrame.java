/**
 * Copyright (c) 2005-2007, KoLmafia development team
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
import java.awt.Component;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CakeArenaManager.ArenaOpponent;

public class CakeArenaFrame extends KoLFrame
{
	private JTable familiarTable;
	private LockableListModel opponents;

	public CakeArenaFrame()
	{
		super( "Susie's Secret Bedroom!" );

		this.framePanel.setLayout( new BorderLayout() );
		this.framePanel.add( new CakeArenaPanel(), BorderLayout.CENTER );
		KoLCharacter.addCharacterListener( new KoLCharacterAdapter( new FamiliarRefresher() ) );
	}

	public UnfocusedTabbedPane getTabbedPane()
	{	return null;
	}

	private class FamiliarRefresher implements Runnable
	{
		public void run()
		{
			if ( CakeArenaFrame.this.familiarTable != null )
				CakeArenaFrame.this.familiarTable.validate();
		}
	}

	private class CakeArenaPanel extends JPanel
	{
		public CakeArenaPanel()
		{
			super( new BorderLayout( 0, 10 ) );
			CakeArenaFrame.this.opponents = CakeArenaManager.getOpponentList();

			String opponentRace;
			String [] columnNames = { "Familiar", "Cage Match", "Scavenger Hunt", "Obstacle Course", "Hide and Seek" };

			// Register the data for your current familiar to be
			// rendered in the table.

			Object [][] familiarData = new Object[1][5];

			CakeArenaFrame.this.familiarTable = new JTable( familiarData, columnNames );
			CakeArenaFrame.this.familiarTable.setRowHeight( 40 );

			for ( int i = 0; i < 5; ++i )
			{
				CakeArenaFrame.this.familiarTable.setDefaultEditor( CakeArenaFrame.this.familiarTable.getColumnClass(i), null );
				CakeArenaFrame.this.familiarTable.setDefaultRenderer( CakeArenaFrame.this.familiarTable.getColumnClass(i), new OpponentRenderer() );
			}

			JPanel familiarPanel = new JPanel( new BorderLayout() );
			familiarPanel.add( CakeArenaFrame.this.familiarTable.getTableHeader(), BorderLayout.NORTH );
			familiarPanel.add( CakeArenaFrame.this.familiarTable, BorderLayout.CENTER );

			Object [][] opponentData = new Object[ CakeArenaFrame.this.opponents.size() ][5];

			// Register the data for your opponents to be rendered
			// in the table, taking into account the offset due to
			// your own familiar's data.

			for ( int i = 0; i < CakeArenaFrame.this.opponents.size(); ++i )
			{
				opponentRace = ((ArenaOpponent)CakeArenaFrame.this.opponents.get(i)).getRace();
				opponentData[i][0] = CakeArenaFrame.this.opponents.get(i).toString();

				for ( int j = 1; j <= 4; ++j )
					opponentData[i][j] = new OpponentButton( i, j, FamiliarsDatabase.getFamiliarSkill( opponentRace, j ) );
			}

			JTable opponentTable = new JTable( opponentData, columnNames );
			opponentTable.addMouseListener( new ButtonEventListener( opponentTable ) );
			opponentTable.setRowHeight( 40 );

			for ( int i = 0; i < 5; ++i )
			{
				opponentTable.setDefaultEditor( opponentTable.getColumnClass(i), null );
				opponentTable.setDefaultRenderer( opponentTable.getColumnClass(i), new OpponentRenderer() );
			}

			JPanel opponentPanel = new JPanel( new BorderLayout() );
			opponentPanel.add( opponentTable.getTableHeader(), BorderLayout.NORTH );
			opponentPanel.add( opponentTable, BorderLayout.CENTER );

			this.add( familiarPanel, BorderLayout.NORTH );
			this.add( opponentPanel, BorderLayout.CENTER );
		}
	}

	private class OpponentButton extends NestedInsideTableButton implements MouseListener
	{
		private int row, column;
		private String opponentSkill;

		public OpponentButton( int row, int column, Integer skill )
		{
			super( JComponentUtilities.getImage( (skill == null ? "0" : skill.toString()) + "star.gif" ) );

			this.row = row;
			this.column = column;
			this.opponentSkill = skill.intValue() == 1 ? "1 star (opponent)" : skill + " stars (opponent)";
		}

		public void mouseReleased( MouseEvent e )
		{
			int yourSkillValue = FamiliarsDatabase.getFamiliarSkill( KoLCharacter.getFamiliar().getRace(), this.column ).intValue();
			String yourSkill = yourSkillValue == 1 ? "1 star (yours)" : yourSkillValue + " stars (yours)";

			int battleCount = StaticEntity.parseInt( input( "<html>" + CakeArenaFrame.this.opponents.get( this.row ).toString() + ", " +
				CakeArenaManager.getEvent( this.column ) + "<br>" + yourSkill + " vs. " + this.opponentSkill + "</html>" ) );

			if ( battleCount > 0 )
				CakeArenaManager.fightOpponent( CakeArenaFrame.this.opponents.get( this.row ).toString(), this.column, battleCount );
		}
	}

	private class OpponentRenderer implements TableCellRenderer
	{
		public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column )
		{	return value == null ? this.getFamiliarComponent( column ) : this.getStandardComponent( value );
		}

		private Component getFamiliarComponent( int column )
		{
			FamiliarData currentFamiliar = KoLCharacter.getFamiliar();

			if ( column == 0 )
				return currentFamiliar == null ? this.getStandardComponent( "NO DATA (0 lbs)" ) :
					this.getStandardComponent( currentFamiliar.toString() );

			return currentFamiliar == null ? new JLabel( JComponentUtilities.getImage( "0star.gif" ) ) :
				new JLabel( JComponentUtilities.getImage( FamiliarsDatabase.getFamiliarSkill( currentFamiliar.getRace(), column ).toString() + "star.gif" ) );
		}

		private Component getStandardComponent( Object value )
		{
			if ( value instanceof OpponentButton )
				return (OpponentButton) value;

			String name = value.toString();

			JPanel component = new JPanel( new BorderLayout() );
			component.add( new JLabel( name.substring( 0, name.indexOf( "(" ) - 1 ), JLabel.CENTER ), BorderLayout.CENTER );
			component.add( new JLabel( name.substring( name.indexOf( "(" ) ), JLabel.CENTER ), BorderLayout.SOUTH );

			return component;
		}
	}
}
