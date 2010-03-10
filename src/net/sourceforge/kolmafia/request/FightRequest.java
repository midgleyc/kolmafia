/**
 * Copyright (c) 2005-2009, KoLmafia development team
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

package net.sourceforge.kolmafia.request;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.MPRestoreItemList;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemFinder;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Monster;
import net.sourceforge.kolmafia.session.CustomCombatManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.RecoveryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.session.TurnCounter;
import net.sourceforge.kolmafia.session.WumpusManager;
import net.sourceforge.kolmafia.swingui.panel.AdventureSelectPanel;
import net.sourceforge.kolmafia.textui.Interpreter;
import net.sourceforge.kolmafia.utilities.PauseObject;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.DiscoCombatHelper;
import net.sourceforge.kolmafia.webui.HobopolisDecorator;
import net.sourceforge.kolmafia.webui.NemesisDecorator;
import net.sourceforge.kolmafia.webui.IslandDecorator;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentToken;
import org.htmlcleaner.ContentToken;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

public class FightRequest
	extends GenericRequest
{
	// Character-class permissions
	private static boolean canSteal = false;
	private static boolean canSummon = false;

	private static final PauseObject PAUSER = new PauseObject();
	public static final FightRequest INSTANCE = new FightRequest();

	private static final AdventureResult AMNESIA = new AdventureResult( "Amnesia", 1, true );
	private static final AdventureResult CUNCTATITIS = new AdventureResult( "Cunctatitis", 1, true );
	public static final AdventureResult ONTHETRAIL = new AdventureResult( "On the Trail", 1, true );
	public static final AdventureResult BIRDFORM = new AdventureResult( "Form of...Bird!", 1, true );
	public static final AdventureResult MOLEFORM = new AdventureResult( "Shape of...Mole!", 1, true );

	public static final AdventureResult DICTIONARY1 = ItemPool.get( ItemPool.DICTIONARY, 1 );
	public static final AdventureResult DICTIONARY2 = ItemPool.get( ItemPool.FACSIMILE_DICTIONARY, 1 );
	private static final AdventureResult SOLDIER = ItemPool.get( ItemPool.TOY_SOLDIER, 1 );
	private static final AdventureResult TEQUILA = ItemPool.get( ItemPool.TEQUILA, -1 );

	private static AdventureResult haikuEffect = EffectPool.get( EffectPool.HAIKU_STATE_OF_MIND );

	private static int lastUserId = 0;
	private static String lostInitiativeMessage = "";
	private static String wonInitiativeMessage = "";

	private static int preparatoryRounds = 0;
	private static String consultScriptThatDidNothing = null;
	private static boolean waitingForSpecial;
	public static String ireallymeanit = null;

	public static String lastResponseText = "";
	private static boolean isTrackingFights = false;
	private static boolean foundNextRound = false;
	private static boolean haveFought = false;
	private static boolean shouldRefresh = false;
	private static boolean haveHaikuResults = false;

	private static boolean isAutomatingFight = false;
	private static boolean isUsingConsultScript = false;
	public static Interpreter filterInterp;
	public static String filterFunction;

	private static final Pattern COMBATITEM_PATTERN = Pattern.compile( "<option[^>]*?value=(\\d+)[^>]*?>[^>]*?\\((\\d+)\\)</option>" );

	public static final Pattern SKILL_PATTERN = Pattern.compile( "whichskill=(\\d+)" );
	private static final Pattern ITEM1_PATTERN = Pattern.compile( "whichitem=(\\d+)" );
	private static final Pattern ITEM2_PATTERN = Pattern.compile( "whichitem2=(\\d+)" );
	private static final Pattern CLEESH_PATTERN =
		Pattern.compile( "You cast CLEESH at your opponent.*?into a (\\w*)", Pattern.DOTALL );
	private static final Pattern WORN_STICKER_PATTERN =
		Pattern.compile( "A sticker falls off your weapon, faded and torn" );
	private static final Pattern BALLROOM_SONG_PATTERN =
		Pattern.compile( "You hear strains of (?:(lively)|(mellow)|(lovely)) music in the distance" );

	private static final Pattern BOSSBAT_PATTERN =
		Pattern.compile( "until he disengages, two goofy grins on his faces.*?You lose ([\\d,]+)" );
	private static final Pattern GHUOL_HEAL = Pattern.compile( "feasts on a nearby corpse, and looks refreshed\\." );
	private static final Pattern NS_HEAL =
		Pattern.compile( "The Sorceress pulls a tiny red vial out of the folds of her dress and quickly drinks it" );
	private static final Pattern DETECTIVE_PATTERN =
		Pattern.compile( "I deduce that this monster has approximately (\\d+) hit points" );
	private static final Pattern SPACE_HELMET_PATTERN =
		Pattern.compile( "Opponent HP: (\\d+)" );
	private static final Pattern SLIMED_PATTERN =
		Pattern.compile( "it blasts you with a massive loogie that sticks to your (.*?), pulls it off of you" );

	private static final AdventureResult TOOTH = ItemPool.get( ItemPool.SEAL_TOOTH, 1);
	private static final AdventureResult TURTLE = ItemPool.get( ItemPool.TURTLE_TOTEM, 1);
	private static final AdventureResult SPICES = ItemPool.get( ItemPool.SPICES, 1);
	private static final AdventureResult MERCENARY = ItemPool.get( ItemPool.TOY_MERCENARY, 1);
	private static final AdventureResult STOMPER = ItemPool.get( ItemPool.MINIBORG_STOMPER, 1);
	private static final AdventureResult LASER = ItemPool.get( ItemPool.MINIBORG_LASER, 1);
	private static final AdventureResult DESTROYER = ItemPool.get( ItemPool.MINIBORG_DESTROYOBOT, 1);
	private static final AdventureResult SHURIKEN = ItemPool.get( ItemPool.PAPER_SHURIKEN, 1);
	private static final AdventureResult ANTIDOTE = ItemPool.get( ItemPool.ANTIDOTE, 1);
	private static final AdventureResult EXTRACTOR = ItemPool.get( ItemPool.ODOR_EXTRACTOR, 1);
	private static final AdventureResult PUTTY_SHEET = ItemPool.get( ItemPool.SPOOKY_PUTTY_SHEET, 1);
	private static final AdventureResult CAMERA = ItemPool.get( ItemPool.CAMERA, 1);
	private static final AdventureResult SHAKING_CAMERA = ItemPool.get( ItemPool.SHAKING_CAMERA, 1);

	private static final String TOOTH_ACTION = "item" + ItemPool.SEAL_TOOTH;
	private static final String TURTLE_ACTION = "item" + ItemPool.TURTLE_TOTEM;
	private static final String SPICES_ACTION = "item" + ItemPool.SPICES;
	private static final String MERCENARY_ACTION = "item" + ItemPool.TOY_MERCENARY;
	private static final String STOMPER_ACTION = "item" + ItemPool.MINIBORG_STOMPER;
	private static final String LASER_ACTION = "item" + ItemPool.MINIBORG_LASER;
	private static final String DESTROYER_ACTION = "item" + ItemPool.MINIBORG_DESTROYOBOT;
	private static final String SHURIKEN_ACTION = "item" + ItemPool.PAPER_SHURIKEN;
	private static final String ANTIDOTE_ACTION = "item" + ItemPool.ANTIDOTE;
	private static final String OLFACTION_ACTION = "skill" + SkillDatabase.OLFACTION;

	private static final AdventureResult BROKEN_GREAVES = ItemPool.get( ItemPool.ANTIQUE_GREAVES, -1 );

	private static boolean castNoodles = false;
	private static boolean castCleesh = false;
	private static boolean jiggledChefstaff = false;
	private static boolean canOlfact = true;
	private static int stealthMistletoe = 1;
	private static boolean summonedGhost = false;
	private static int currentRound = 0;
	private static int levelModifier = 0;
	private static int healthModifier = 0;

	private static String action1 = null;
	private static String action2 = null;
	private static Monster monsterData = null;
	private static String encounterLookup = "";

	private static AdventureResult desiredScroll = null;

	private static final AdventureResult SCROLL_334 = ItemPool.get( ItemPool.SCROLL_334, 1);
	public static final AdventureResult SCROLL_668 = ItemPool.get( ItemPool.SCROLL_668, 1);
	private static final AdventureResult SCROLL_30669 = ItemPool.get( ItemPool.SCROLL_30669, 1);
	private static final AdventureResult SCROLL_33398 = ItemPool.get( ItemPool.SCROLL_33398, 1);
	private static final AdventureResult SCROLL_64067 = ItemPool.get( ItemPool.SCROLL_64067, 1);
	public static final AdventureResult SCROLL_64735 = ItemPool.get( ItemPool.GATES_SCROLL, 1);
	public static final AdventureResult SCROLL_31337 = ItemPool.get( ItemPool.ELITE_SCROLL, 1);
	
	private static final Object[][] NEMESIS_WEAPONS =
	{	// class, LEW, ULEW
		{
			KoLCharacter.SEAL_CLUBBER,
			ItemPool.get( ItemPool.HAMMER_OF_SMITING, 1 ),
			ItemPool.get( ItemPool.SLEDGEHAMMER_OF_THE_VAELKYR, 1 )
		},
		{
			KoLCharacter.TURTLE_TAMER,
			ItemPool.get( ItemPool.CHELONIAN_MORNINGSTAR, 1 ),
			ItemPool.get( ItemPool.FLAIL_OF_THE_SEVEN_ASPECTS, 1 )
		},
		{
			KoLCharacter.PASTAMANCER,
			ItemPool.get( ItemPool.GREEK_PASTA_OF_PERIL, 1 ),
			ItemPool.get( ItemPool.WRATH_OF_THE_PASTALORDS, 1 )
		},
		{
			KoLCharacter.SAUCEROR,
			ItemPool.get( ItemPool.SEVENTEEN_ALARM_SAUCEPAN, 1 ),
			ItemPool.get( ItemPool.WINDSOR_PAN_OF_THE_SOURCE, 1 )
		},
		{
			KoLCharacter.DISCO_BANDIT,
			ItemPool.get( ItemPool.SHAGADELIC_DISCO_BANJO, 1 ),
			ItemPool.get( ItemPool.SEEGERS_BANJO, 1 )
		},
		{
			KoLCharacter.ACCORDION_THIEF,
			ItemPool.get( ItemPool.SQUEEZEBOX_OF_THE_AGES, 1 ),
			ItemPool.get( ItemPool.TRICKSTER_TRIKITIXA, 1 )
		},
	};

	// Ultra-rare monsters
	private static final String[] RARE_MONSTERS =
	{
		"baiowulf",
		"count bakula",
		"crazy bastard",
		"hockey elemental",
		"hypnotist of hey deze",
		"infinite meat bug",
		"knott slanding",
		"master of thieves",
		"temporal bandit"
	};

	// Skills which cannot be used with a ranged weapon
	private static final HashSet INVALID_WITH_RANGED_ATTACK = new HashSet();
	static
	{
		INVALID_WITH_RANGED_ATTACK.add( "1003" );
		INVALID_WITH_RANGED_ATTACK.add( "skill thrust-smack" );
		INVALID_WITH_RANGED_ATTACK.add( "1005" );
		INVALID_WITH_RANGED_ATTACK.add( "skill lunging thrust-smack" );
		INVALID_WITH_RANGED_ATTACK.add( "2003" );
		INVALID_WITH_RANGED_ATTACK.add( "skill headbutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2005" );
		INVALID_WITH_RANGED_ATTACK.add( "skill shieldbutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2015" );
		INVALID_WITH_RANGED_ATTACK.add( "skill kneebutt" );
		INVALID_WITH_RANGED_ATTACK.add( "2103" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + knee combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2105" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + shield combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2106" );
		INVALID_WITH_RANGED_ATTACK.add( "skill knee + shield combo" );
		INVALID_WITH_RANGED_ATTACK.add( "2107" );
		INVALID_WITH_RANGED_ATTACK.add( "skill head + knee + shield combo" );
	}

	// Skills which require a shield
	private static final HashSet INVALID_WITHOUT_SHIELD = new HashSet();
	static
	{
		INVALID_WITHOUT_SHIELD.add( "2005" );
		INVALID_WITHOUT_SHIELD.add( "skill shieldbutt" );
		INVALID_WITHOUT_SHIELD.add( "2105" );
		INVALID_WITHOUT_SHIELD.add( "skill head + shield combo" );
		INVALID_WITHOUT_SHIELD.add( "2106" );
		INVALID_WITHOUT_SHIELD.add( "skill knee + shield combo" );
		INVALID_WITHOUT_SHIELD.add( "2107" );
		INVALID_WITHOUT_SHIELD.add( "skill head + knee + shield combo" );
	}

	private static final HashSet INVALID_OUT_OF_WATER = new HashSet();
	static
	{
		INVALID_OUT_OF_WATER.add( "1023" );
		INVALID_OUT_OF_WATER.add( "skill harpoon!" );
		INVALID_OUT_OF_WATER.add( "2024" );
		INVALID_OUT_OF_WATER.add( "skill summon leviatuga" );
	}

	// Make an HTML cleaner
	private static HtmlCleaner cleaner = new HtmlCleaner();

	static
	{
		CleanerProperties props = FightRequest.cleaner.getProperties();
		props.setTranslateSpecialEntities( false );
		props.setRecognizeUnicodeChars( false );
		props.setOmitXmlDeclaration( true );
	}

	/**
	 * Constructs a new <code>FightRequest</code>. User settings will be
	 * used to determine the kind of action to be taken during the battle.
	 */

	private FightRequest()
	{
		super( "fight.php" );
	}

	public static final void initialize()
	{
		FightRequest.canSteal = KoLCharacter.isMoxieClass();
		FightRequest.canSummon = KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER;
	}

	protected boolean retryOnTimeout()
	{
		return true;
	}

	private static final Pattern CAN_STEAL_PATTERN =
		Pattern.compile( "value=\"(Pick (?:His|Her|Their|Its) Pocket(?: Again)?|Look for Shiny Objects)\"" );

	public static final boolean canStillSteal()
	{
		// Return true if you can still steal during this battle.

		// Must be a Moxie class character or any character in Birdform
		if ( !( FightRequest.canSteal || KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) ) )
		{
			return false;
		}

		// Look for buttons that allow you to pickpocket
		String responseText = FightRequest.lastResponseText;
		Matcher matcher = FightRequest.CAN_STEAL_PATTERN.matcher( responseText );
		return matcher.find();
	}

	public static final boolean canCastNoodles()
	{
		return !FightRequest.castNoodles;
	}

	public static final boolean canOlfact()
	{
		return FightRequest.canOlfact && !KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL );

	}

	public static final boolean canStillSummon()
	{
		// Return true if you can still summon during this battle.

		// Must be a Pastamancer
		if ( !FightRequest.canSummon )
		{
			return false;
		}

		// Look for active buttons that allow you to summon
		// ***

		if ( !FightRequest.wonInitiative() )
		{
			return false;
		}

		// Check daily summon limit
		int summons = Preferences.getInteger( "pastamancerGhostSummons" );
		int limit = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPAGHETTI_BANDOLIER, 1 ) ) ? 10 : 15;

		return ( summons < limit );
	}

	public static final boolean wonInitiative()
	{
		return	FightRequest.currentRound == 1 &&
			FightRequest.wonInitiative( FightRequest.lastResponseText );
	}

	public static final boolean wonInitiative( String text )
	{
		// Regular encounter
		if ( text.indexOf( "You get the jump" ) != -1 )
			return true;

		// Can Has Cyborger
		if ( text.indexOf( "The Jump: " ) != -1 )
			return true;

		// Haiku dungeon

		//    Before he sees you,
		//    you're already attacking.
		//    You're sneaky like that.

		if ( text.indexOf( "You're sneaky like that." ) != -1 )
			return true;

		//    You leap at your foe,
		//    throwing caution to the wind,
		//    and get the first strike.

		if ( text.indexOf( "and get the first strike." ) != -1 )
			return true;

		//    You jump at your foe
		//    and strike before he's ready.
		//    Nice and sportsmanlike.

		if ( text.indexOf( "Nice and sportsmanlike." ) != -1 )
			return true;

		return false;
	}

	public void nextRound()
	{
		// When logging in and encountering a fight, always use the
		// attack command to avoid abort problems.

		if ( LoginRequest.isInstanceRunning() )
		{
			FightRequest.action1 = "attack";
			this.addFormField( "action", "attack" );
			return;
		}

		if ( KoLmafia.refusesContinue() )
		{
			FightRequest.action1 = "abort";
			return;
		}

		// First round, KoLmafia does not decide the action.
		// Update accordingly.

		if ( FightRequest.currentRound == 0 )
		{
			FightRequest.action1 = null;
			if ( FightRequest.ireallymeanit != null )
			{
				this.addFormField( "ireallymeanit", FightRequest.ireallymeanit );
				FightRequest.ireallymeanit = null;
			}
			return;
		}

		// Always let the user see rare monsters

		for ( int i = 0; i < FightRequest.RARE_MONSTERS.length; ++i )
		{
			if ( FightRequest.encounterLookup.indexOf( FightRequest.RARE_MONSTERS[ i ] ) != -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You have encountered the " + this.encounter );
				FightRequest.action1 = "abort";
				return;
			}
		}

		// Fight automation is still considered automation.
		// If the player drops below the threshold, then go
		// ahead and halt the battle.

		if ( !RecoveryManager.runThresholdChecks() )
		{
			FightRequest.action1 = "abort";
			return;
		}

		this.nextRound( null );
	}

	public void nextRound( String desiredAction )
	{
		FightRequest.action1 = null;
		FightRequest.action2 = null;

		// Adding machine should override custom combat scripts as well,
		// since it's conditions-driven.

		if ( FightRequest.encounterLookup.equals( "rampaging adding machine" )
			&& !KoLConstants.activeEffects.contains( FightRequest.BIRDFORM )
			&& !FightRequest.waitingForSpecial )
		{
			this.handleAddingMachine();
		}

		// Hulking Constructs also require special handling

		else if ( FightRequest.encounterLookup.equals( "hulking construct" ) )
		{
			this.handleHulkingConstruct();
		}

		if ( FightRequest.action1 == null )
		{
			if ( desiredAction == null )
			{
				int index = FightRequest.currentRound - 1 - FightRequest.preparatoryRounds;
				if ( FightRequest.filterInterp != null )
				{
					desiredAction = FightRequest.filterInterp.execute(
						FightRequest.filterFunction, new String[]
							{
								String.valueOf( index ),
								FightRequest.encounterLookup,
								FightRequest.lastResponseText
							}, false ).toString();
					if ( KoLmafia.refusesContinue() )
					{
						FightRequest.action1 = "abort";
						return;
					}
				}
				else
				{
					desiredAction = CustomCombatManager.getSetting(
						FightRequest.encounterLookup, index );
				}
			}
			FightRequest.action1 =
				CustomCombatManager.getShortCombatOptionName( desiredAction );
		}

		// If the person wants to use their own script,
		// then this is where it happens.

		if ( FightRequest.action1.startsWith( "consult" ) )
		{
			FightRequest.isUsingConsultScript = true;
			String scriptName = FightRequest.action1.substring( "consult".length() ).trim();

			Interpreter interpreter = KoLmafiaASH.getInterpreter( KoLmafiaCLI.findScriptFile( scriptName ) );
			if ( interpreter != null )
			{
				int initialRound = FightRequest.currentRound;
				interpreter.execute( "main", new String[]
				{
					String.valueOf( FightRequest.currentRound ),
					FightRequest.encounterLookup,
					FightRequest.lastResponseText
				} );

				if ( KoLmafia.refusesContinue() )
				{
					FightRequest.action1 = "abort";
				}
				else if ( initialRound == FightRequest.currentRound )
				{
					if ( FightRequest.action1.equals( FightRequest.consultScriptThatDidNothing ) )
					{
						FightRequest.action1 = "abort";
					}
					else
					{
						FightRequest.consultScriptThatDidNothing = FightRequest.action1;
						--FightRequest.preparatoryRounds;
					}
				}

				return;
			}

			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Consult script '" + scriptName + "' not found." );
			FightRequest.action1 = "abort";
			return;
		}

		// Let the de-level action figure out what
		// should be done, and then re-process.

		if ( FightRequest.action1.startsWith( "delevel" ) )
		{
			FightRequest.action1 = this.getMonsterWeakenAction();
		}

		this.updateCurrentAction();
	}

	public static final String getCurrentKey()
	{
		return CustomCombatManager.encounterKey( FightRequest.encounterLookup );
	}

	private void updateCurrentAction()
	{
		if ( FightRequest.shouldUseAntidote() )
		{
			FightRequest.action1 = String.valueOf( ItemPool.ANTIDOTE );
			++FightRequest.preparatoryRounds;
		}

		if ( FightRequest.action1.equals( "special" ) )
		{
			FightRequest.waitingForSpecial = false;
			if ( GenericRequest.passwordHash.equals( "" ) || !FightRequest.getSpecialAction() )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}
		}

		if ( FightRequest.action1.equals( "abort" ) )
		{
			// If the user has chosen to abort combat, flag it.
			--FightRequest.preparatoryRounds;
			return;
		}

		if ( FightRequest.action1.equals( "abort after" ) )
		{
			KoLmafia.abortAfter( "Aborted by CCS request" );
			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		if ( FightRequest.action1.equals( "skip" ) )
		{
			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// User wants to run away
		if ( FightRequest.action1.indexOf( "run" ) != -1 && FightRequest.action1.indexOf( "away" ) != -1 )
		{
			Matcher runAwayMatcher = CustomCombatManager.TRY_TO_RUN_AWAY_PATTERN.matcher( FightRequest.action1 );

			int runaway = 0;

			if ( runAwayMatcher.find() )
			{
				runaway = StringUtilities.parseInt( runAwayMatcher.group( 1 ) );
			}

			FightRequest.action1 = "runaway";

			if ( runaway > FightRequest.freeRunawayChance() )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}

			this.addFormField( "action", FightRequest.action1 );
			return;
		}

		// User wants a regular attack
		if ( FightRequest.action1.startsWith( "attack" ) )
		{
			FightRequest.action1 = "attack";
			this.addFormField( "action", FightRequest.action1 );
			return;
		}

		if ( FightRequest.action1.startsWith( "twiddle" ) )
		{
			FightRequest.action1 = null;
			return;
		}

		if ( KoLConstants.activeEffects.contains( FightRequest.AMNESIA ) )
		{
			if ( FightRequest.monsterData == null || !FightRequest.monsterData.willUsuallyMiss( FightRequest.levelModifier ) )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.action1 = "abort";
			return;
		}

		// Actually steal if the action says to steal

		if ( FightRequest.action1.indexOf( "steal" ) != -1 &&
		     FightRequest.action1.indexOf( "stealth" ) == -1 )
		{
			if ( FightRequest.canStillSteal() &&
			     FightRequest.monsterData != null &&
			     FightRequest.monsterData.shouldSteal() )
			{
				FightRequest.action1 = "steal";
				this.addFormField( "action", "steal" );
				return;
			}

			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// Summon a ghost if requested.

		if ( FightRequest.action1.equals( "summon ghost" ) )
		{
			if ( FightRequest.canStillSummon() )
			{
				this.addFormField( "action", "summon" );
				return;
			}

			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// Jiggle chefstaff if the action says to jiggle and we're
		// wielding a chefstaff. Otherwise, skip this action.

		if ( FightRequest.action1.startsWith( "jiggle" ) )
		{
			if ( !FightRequest.jiggledChefstaff &&
			     EquipmentManager.usingChefstaff() )
			{
				this.addFormField( "action", "chefstaff" );
				return;
			}

			// You can only jiggle once per round.
			--FightRequest.preparatoryRounds;
			this.nextRound();
			return;
		}

		// If the player wants to use an item, make sure he has one
		if ( !FightRequest.action1.startsWith( "skill" ) )
		{
			if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
			{	// Can't use items in Birdform
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}
			int item1, item2;

			int commaIndex = FightRequest.action1.indexOf( "," );
			if ( commaIndex != -1 )
			{
				item1 = StringUtilities.parseInt( FightRequest.action1.substring( 0, commaIndex ) );
				item2 = StringUtilities.parseInt( FightRequest.action1.substring( commaIndex + 1 ) );
			}
			else
			{
				item1 = StringUtilities.parseInt( FightRequest.action1 );
				item2 = -1;
			}

			int itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );

			if ( itemCount == 0 && item2 != -1)
			{
				item1 = item2;
				item2 = -1;

				itemCount = ( new AdventureResult( item1, 1 ) ).getCount( KoLConstants.inventory );
			}

			if ( itemCount == 0 )
			{
				KoLmafia.updateDisplay(
					KoLConstants.ABORT_STATE, "You don't have enough " + ItemDatabase.getItemName( item1 ) );
				FightRequest.action1 = "abort";
				return;
			}

			if ( item1 == ItemPool.DICTIONARY || item1 == ItemPool.FACSIMILE_DICTIONARY )
			{
				if ( itemCount < 1 )
				{
					KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You don't have a dictionary." );
					FightRequest.action1 = "abort";
					return;
				}

				if ( FightRequest.encounterLookup.equals( "rampaging adding machine" ) )
				{
					FightRequest.action1 = "attack";
					this.addFormField( "action", FightRequest.action1 );
					return;
				}
			}

			this.addFormField( "action", "useitem" );
			this.addFormField( "whichitem", String.valueOf( item1 ) );

			if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
			{
				return;
			}

			if ( item2 != -1 )
			{
				itemCount = ( new AdventureResult( item2, 1 ) ).getCount( KoLConstants.inventory );

				if ( itemCount > 1 || item1 != item2 && itemCount > 0 )
				{
					FightRequest.action2 = String.valueOf( item2 );
					this.addFormField( "whichitem2", String.valueOf( item2 ) );
				}
				else
				{
					KoLmafia.updateDisplay(
						KoLConstants.ABORT_STATE, "You don't have enough " + ItemDatabase.getItemName( item2 ) );
					FightRequest.action1 = "abort";
				}

				return;
			}

			if ( singleUseCombatItem( item1 ) )
			{
				if ( KoLConstants.inventory.contains( FightRequest.MERCENARY ) )
				{
					FightRequest.action2 = FightRequest.MERCENARY_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.MERCENARY.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.DESTROYER ) )
				{
					FightRequest.action2 = FightRequest.DESTROYER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.DESTROYER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.LASER ) )
				{
					FightRequest.action2 = FightRequest.LASER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.LASER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.STOMPER ) )
				{
					FightRequest.action2 = FightRequest.STOMPER_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.STOMPER.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.TOOTH ) )
				{
					FightRequest.action2 = FightRequest.TOOTH_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.TOOTH.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.TURTLE ) )
				{
					FightRequest.action2 = FightRequest.TURTLE_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.TURTLE.getItemId() ) );
				}
				else if ( KoLConstants.inventory.contains( FightRequest.SPICES ) )
				{
					FightRequest.action2 = FightRequest.SPICES_ACTION;
					this.addFormField( "whichitem2", String.valueOf( FightRequest.SPICES.getItemId() ) );
				}
			}
			else if ( itemCount >= 2 && !soloUseCombatItem( item1 ))
			{
				FightRequest.action2 = FightRequest.action1;
				this.addFormField( "whichitem2", String.valueOf( item1 ) );
			}

			return;
		}

		// We do not verify that the character actually knows the skill
		// or that it is currently available. It can be complicated:
		// birdform skills are available only in birdform., but some
		// are available only if you've prepped them by eating the
		// appropriate kind of bug.

		// We do ensure that it is a combat skill.

		String skillName =
			SkillDatabase.getSkillName( StringUtilities.parseInt( FightRequest.action1.substring( 5 ) ) );

		if ( SkillDatabase.getCombatSkillName( skillName ) == null )
		{
			if ( this.isAcceptable( 0, 0 ) )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.action1 = "abort";
			return;
		}

		if ( skillName.equals( "Transcendent Olfaction" ) )
		{
			// You can't sniff if you are already on the trail.

			// You can't sniff in Bad Moon, even though the skill
			// shows up on the char sheet, unless you've recalled
			// your skills.

			if ( ( KoLCharacter.inBadMoon() && !KoLCharacter.skillsRecalled() ) || KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.ON_THE_TRAIL ) ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}
		}
		else if ( skillName.equals( "Consume Burrowgrub" ) )
		{
			// You can only consume 3 burrowgrubs per day

			if ( Preferences.getInteger( "burrowgrubSummonsRemaining" ) <= 0 )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}
		}
		else if ( skillName.equals( "Entangling Noodles" ) )
		{
			// You can only use this skill once per combat

			if ( FightRequest.castNoodles )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}

			FightRequest.castNoodles = true;
		}

		// Skills use MP. Make sure the character has enough.
		if ( KoLCharacter.getCurrentMP() < FightRequest.getActionCost() && !GenericRequest.passwordHash.equals( "" ) )
		{
			if ( !Preferences.getBoolean( "autoManaRestore" ) )
			{
				--FightRequest.preparatoryRounds;
				this.nextRound();
				return;
			}

			for ( int i = 0; i < MPRestoreItemList.CONFIGURES.length; ++i )
			{
				if ( MPRestoreItemList.CONFIGURES[ i ].isCombatUsable() && KoLConstants.inventory.contains( MPRestoreItemList.CONFIGURES[ i ].getItem() ) )
				{
					FightRequest.action1 = String.valueOf( MPRestoreItemList.CONFIGURES[ i ].getItem().getItemId() );

					++FightRequest.preparatoryRounds;
					this.updateCurrentAction();
					return;
				}
			}

			FightRequest.action1 = "abort";
			return;
		}

		if ( skillName.equals( "CLEESH" ) )
		{
			if ( FightRequest.castCleesh )
			{
				FightRequest.action1 = "attack";
				this.addFormField( "action", FightRequest.action1 );
				return;
			}

			FightRequest.castCleesh = true;
		}

		if ( FightRequest.isInvalidAttack( FightRequest.action1 ) )
		{
			FightRequest.action1 = "abort";
			return;
		}

		this.addFormField( "action", "skill" );
		this.addFormField( "whichskill", FightRequest.action1.substring( 5 ) );
	}

	private boolean singleUseCombatItem( int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_SINGLE );
	}

	private boolean soloUseCombatItem( int itemId )
	{
		return ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_SOLO );
	}

	public static final boolean isInvalidRangedAttack( final String action )
	{
		if ( !INVALID_WITH_RANGED_ATTACK.contains( action.toLowerCase() ) )
		{
			return false;
		}

		int weaponId = EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId();

		if ( EquipmentDatabase.getWeaponType( weaponId ) == KoLConstants.RANGED )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "This skill is useless with ranged weapons." );
			return true;
		}

		return false;
	}

	public static final boolean isInvalidShieldlessAttack( final String action )
	{
		if ( !INVALID_WITHOUT_SHIELD.contains( action.toLowerCase() ) )
		{
			return false;
		}

		if ( !EquipmentManager.usingShield() )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "This skill is useless without a shield." );
			return true;
		}

		return false;
	}

	public static final boolean isInvalidLocationAttack( final String action )
	{
		if ( !INVALID_OUT_OF_WATER.contains( action.toLowerCase() ) )
		{
			return false;
		}

		KoLAdventure location = KoLAdventure.lastVisitedLocation();
		String zone = location != null ? location.getZone() : null;

		if ( zone != null && !zone.equals( "The Sea" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "This skill is useless out of water." );
			return true;
		}

		return false;
	}

	public static final boolean isInvalidAttack( final String action )
	{
		return FightRequest.isInvalidRangedAttack( action ) ||
		       FightRequest.isInvalidShieldlessAttack( action ) ||
		       FightRequest.isInvalidLocationAttack( action );
	}

	public void runOnce( final String desiredAction )
	{
		this.clearDataFields();

		FightRequest.action1 = null;
		FightRequest.action2 = null;
		FightRequest.isUsingConsultScript = false;

		if ( !KoLmafia.refusesContinue() )
		{
			if ( desiredAction == null )
			{
				this.nextRound();
			}
			else
			{
				this.nextRound( desiredAction );
			}
		}

		if ( !FightRequest.isUsingConsultScript )
		{
			if ( FightRequest.currentRound == 0 )
			{
				super.run();
			}
			else if ( FightRequest.action1 != null && !FightRequest.action1.equals( "abort" ) )
			{
				super.run();
			}
		}

		if ( FightRequest.action1 != null && FightRequest.action1.equals( "abort" ) )
		{
			KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "You're on your own, partner." );
		}
	}

	public void run()
	{
		RequestThread.openRequestSequence();
		FightRequest.isAutomatingFight = true;

		do
		{
			this.runOnce( null );
		}
		while ( this.responseCode == 200 && FightRequest.currentRound != 0 && !KoLmafia.refusesContinue() );

		if ( this.responseCode == 302 )
		{
			FightRequest.clearInstanceData();
		}

		if ( KoLmafia.refusesContinue() && FightRequest.currentRound != 0
			&& !FightRequest.isTrackingFights() )
		{
			this.showInBrowser( true );
		}

		FightRequest.isAutomatingFight = false;
		RequestThread.closeRequestSequence();
	}

	public static final boolean processResults( final String responseText )
	{
		return FightRequest.shouldRefresh;
	}

	public static boolean haveHaikuResults( final String responseText )
	{
		// Adventuring in the Haiku Dungeon
		// Currently have Haiku State of Mind
		// Just acquired Haiku State of Mind
		FightRequest.haveHaikuResults =
			KoLAdventure.lastAdventureId() == 138 ||
			KoLConstants.activeEffects.contains( FightRequest.haikuEffect ) ||
			responseText.indexOf( EffectPool.HAIKU_STATE_OF_MIND ) != -1 ;

		return FightRequest.haveHaikuResults;
	}

	public static boolean haveHaikuResults()
	{
		return FightRequest.haveHaikuResults;
	}

	public static final int getMonsterLevelModifier()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.levelModifier;
	}

	public static final int getMonsterHealth()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getAdjustedHP( KoLCharacter.getMonsterLevelAdjustment() ) - FightRequest.healthModifier;
	}

	public static final int getMonsterAttack()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getAttack() + FightRequest.levelModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterDefense()
	{
		if ( FightRequest.monsterData == null )
		{
			return 0;
		}

		return FightRequest.monsterData.getDefense() + FightRequest.levelModifier + KoLCharacter.getMonsterLevelAdjustment();
	}

	public static final int getMonsterAttackElement()
	{
		if ( FightRequest.monsterData == null )
		{
			return MonsterDatabase.NONE;
		}

		return FightRequest.monsterData.getAttackElement();
	}

	public static final int getMonsterDefenseElement()
	{
		if ( FightRequest.monsterData == null )
		{
			return MonsterDatabase.NONE;
		}

		return FightRequest.monsterData.getDefenseElement();
	}

	public static final boolean willUsuallyMiss()
	{
		return FightRequest.willUsuallyMiss( 0 );
	}

	public static final boolean willUsuallyMiss( final int defenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return false;
		}

		return FightRequest.monsterData.willUsuallyMiss( FightRequest.levelModifier + defenseModifier );
	}

	public static final boolean willUsuallyDodge()
	{
		return FightRequest.willUsuallyDodge( 0 );
	}

	public static final boolean willUsuallyDodge( final int offenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return false;
		}

		return FightRequest.monsterData.willUsuallyDodge( FightRequest.levelModifier + offenseModifier );
	}

	private boolean isAcceptable( final int offenseModifier, final int defenseModifier )
	{
		if ( FightRequest.monsterData == null )
		{
			return true;
		}

		if ( FightRequest.willUsuallyMiss( defenseModifier ) ||
		     FightRequest.willUsuallyDodge( offenseModifier ) )
		{
			return false;
		}

		return RecoveryManager.getRestoreCount() == 0;
	}

	private void handleAddingMachine()
	{
		int action = Preferences.getInteger( "addingScrolls" );
		// 0: show in browser
		// 1: create goal scrolls only
		// 2: create goal & 668
		// 3: create goal, 31337, 668
		if ( action == 0 )
		{
			FightRequest.action1 = "abort";
			return;
		}
		else if ( FightRequest.desiredScroll != null )
		{
			this.createAddingScroll( FightRequest.desiredScroll );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_64735 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64735 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_64067 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_64067 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_31337 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else if ( KoLConstants.conditions.contains( FightRequest.SCROLL_668 ) )
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
		else if ( action >= 3 )
		{
			this.createAddingScroll( FightRequest.SCROLL_31337 );
		}
		else if ( action >= 2 )
		{
			this.createAddingScroll( FightRequest.SCROLL_668 );
		}
	}

	private boolean createAddingScroll( final AdventureResult scroll )
	{
		AdventureResult part1 = null;
		AdventureResult part2 = null;

		if ( scroll == FightRequest.SCROLL_64735 )
		{
			part2 = FightRequest.SCROLL_64067;
			part1 = FightRequest.SCROLL_668;
		}
		else if ( scroll == FightRequest.SCROLL_64067 )
		{
			if ( !KoLConstants.conditions.contains( FightRequest.SCROLL_64067 ) && KoLConstants.inventory.contains( FightRequest.SCROLL_64067 ) )
			{
				return false;
			}

			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_33398;
		}
		else if ( scroll == FightRequest.SCROLL_668 )
		{
			part1 = FightRequest.SCROLL_334;
			part2 = FightRequest.SCROLL_334;
		}
		else if ( scroll == FightRequest.SCROLL_31337 )
		{
			part1 = FightRequest.SCROLL_30669;
			part2 = FightRequest.SCROLL_668;
		}
		else
		{
			return false;
		}

		if ( FightRequest.desiredScroll != null )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = String.valueOf( part2.getItemId() );

			FightRequest.desiredScroll = null;
			return true;
		}

		if ( part1 == part2 && part1.getCount( KoLConstants.inventory ) < 2 )
		{
			return false;
		}

		if ( !KoLConstants.inventory.contains( part1 ) )
		{
			return this.createAddingScroll( part1 ) || this.createAddingScroll( part2 );
		}

		if ( !KoLConstants.inventory.contains( part2 ) )
		{
			return this.createAddingScroll( part2 );
		}

		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = String.valueOf( part1.getItemId() );

			FightRequest.desiredScroll = scroll;
			return true;
		}

		++FightRequest.preparatoryRounds;
		FightRequest.action1 = part1.getItemId() + "," + part2.getItemId();
		return true;
	}

	private void handleHulkingConstruct()
	{
		if ( FightRequest.currentRound > 1 )
		{
			++FightRequest.preparatoryRounds;
			FightRequest.action1 = "3155";
			return;
		}

		AdventureResult card1 = ItemPool.get( ItemPool.PUNCHCARD_ATTACK, 1 );
		AdventureResult card2 = ItemPool.get( ItemPool.PUNCHCARD_WALL, 1 );

		if ( !KoLConstants.inventory.contains( card1 ) ||
		     !KoLConstants.inventory.contains( card2 ) )
		{
			FightRequest.action1 = "runaway";
			return;
		}

		++FightRequest.preparatoryRounds;
		if ( !KoLCharacter.hasSkill( "Ambidextrous Funkslinging" ) )
		{
			FightRequest.action1 = "3146";
		}
		else
		{
			FightRequest.action1 = "3146,3155";
		}
	}

	private String getMonsterWeakenAction()
	{
		if ( this.isAcceptable( 0, 0 ) )
		{
			return "attack";
		}

		int desiredSkill = 0;
		boolean isAcceptable = false;

		// Disco Eye-Poke
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Eye-Poke" ) )
		{
			desiredSkill = 5003;
			isAcceptable = this.isAcceptable( -1, -1 );
		}

		// Disco Dance of Doom
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance of Doom" ) )
		{
			desiredSkill = 5005;
			isAcceptable = this.isAcceptable( -3, -3 );
		}

		// Disco Dance II: Electric Boogaloo
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Dance II: Electric Boogaloo" ) )
		{
			desiredSkill = 5008;
			isAcceptable = this.isAcceptable( -5, -5 );
		}

		// Tango of Terror
		if ( !isAcceptable && KoLCharacter.hasSkill( "Tango of Terror" ) )
		{
			desiredSkill = 5019;
			isAcceptable = this.isAcceptable( -6, -6 );
		}

		// Disco Face Stab
		if ( !isAcceptable && KoLCharacter.hasSkill( "Disco Face Stab" ) )
		{
			desiredSkill = 5012;
			isAcceptable = this.isAcceptable( -7, -7 );
		}

		return desiredSkill == 0 ? "attack" : "skill" + desiredSkill;
	}

	private static final boolean checkForInitiative( final String responseText )
	{
		if ( FightRequest.isAutomatingFight )
		{
			String action = Preferences.getString( "battleAction" );

			if ( action.startsWith( "custom" ) )
			{
				String file = Preferences.getBoolean( "debugPathnames" ) ? CustomCombatManager.getFile().getAbsolutePath() : CustomCombatManager.getScript();
				action = file + " [" + CustomCombatManager.getSettingKey( FightRequest.encounterLookup ) + "]";
			}

			RequestLogger.printLine( "Strategy: " + action );
		}

		if ( FightRequest.lastUserId != KoLCharacter.getUserId() )
		{
			FightRequest.lastUserId = KoLCharacter.getUserId();
			FightRequest.lostInitiativeMessage = "Round 0: " + KoLCharacter.getUserName() + " loses initiative!";
			FightRequest.wonInitiativeMessage = "Round 0: " + KoLCharacter.getUserName() + " wins initiative!";
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );

		// The response tells you if you won initiative.

		if ( !FightRequest.wonInitiative( responseText ) )
		{
			// If you lose initiative, there's nothing very
			// interesting to print to the session log.

			if ( shouldLogAction )
			{
				RequestLogger.printLine( FightRequest.lostInitiativeMessage );
				RequestLogger.updateSessionLog( FightRequest.lostInitiativeMessage );
			}

			return false;
		}

		// Now that you've won initiative, figure out what actually
		// happened in that first round based on player settings.

		if ( shouldLogAction )
		{
			RequestLogger.printLine( FightRequest.wonInitiativeMessage );
			RequestLogger.updateSessionLog( FightRequest.wonInitiativeMessage );
		}

		FightRequest.action1 = Preferences.getString( "defaultAutoAttack" );

		// If no default action is made by the player, then the round
		// remains the same.  Simply report winning/losing initiative.

		if ( FightRequest.action1.equals( "" ) || FightRequest.action1.equals( "0" ) )
		{
			return false;
		}

		StringBuffer action = new StringBuffer();

		if ( shouldLogAction )
		{
			action.append( "Round 1: " );
			action.append( KoLCharacter.getUserName() );
			action.append( " " );
		}

		if ( FightRequest.action1.equals( "1" ) )
		{
			if ( shouldLogAction )
			{
				action.append( "attacks!" );
			}

			FightRequest.action1 = "attack";
		}
		else if ( FightRequest.action1.equals( "3" ) )
		{
			if ( shouldLogAction )
			{
				action.append( "tries to steal an item!" );
			}

			FightRequest.action1 = "steal";
		}
		else if ( shouldLogAction )
		{
			action.append( "casts " + SkillDatabase.getSkillName( Integer.parseInt( FightRequest.action1 ) ).toUpperCase() + "!" );
		}

		if ( shouldLogAction )
		{
			action.append( " (auto-attack)" );
			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		if ( FightRequest.action1.equals( "3004" ) )
		{
			FightRequest.castNoodles = true;
		}

		return true;
	}

	private static final Pattern ONTURN_PATTERN = Pattern.compile( "onturn = (\\d+)" );

	public static final void updateCombatData( final String location, String encounter, final String responseText )
	{
		FightRequest.lastResponseText = responseText;

		FightRequest.parseBangPotion( responseText );
		FightRequest.parseStoneSphere( responseText );
		FightRequest.parsePirateInsult( responseText );

		FightRequest.parseGrubUsage( location, responseText );
		FightRequest.parseGhostSummoning( location, responseText );
		FightRequest.parseFlyerUsage( location, responseText );

		// Spend MP and consume items
		FightRequest.payActionCost( responseText );

		boolean autoAttacked = false;

		// We've started a new round
		++FightRequest.currentRound;

		// Sanity check: compare our round with what KoL claims it is
		Matcher m = ONTURN_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			int round = StringUtilities.parseInt( m.group(1) );
			if ( round != FightRequest.currentRound )
			{
				RequestLogger.printLine( "KoLmafia thinks it is round " + FightRequest.currentRound + " but KoL thinks it is round " + round );
			}
		}

		// Track disco skill sequences
		DiscoCombatHelper.parseFightRound( location, responseText );

		if ( FightRequest.currentRound == 1 )
		{
			FightRequest.haveFought = true;
			FightRequest.haveHaikuResults = false;
			
			if ( responseText.indexOf( "There is a blinding flash of light, and a chorus of heavenly voices rises in counterpoint to the ominous organ music." ) != -1 )
			{
				FightRequest.transmogrifyNemesisWeapon( false );
			}

			// Increment stinky cheese counter
			int stinkyCount = EquipmentManager.getStinkyCheeseLevel();
			if ( stinkyCount > 0 )
			{
				Preferences.increment( "_stinkyCheeseCount", stinkyCount );
			}

			// If this is the first round, then register the
			// opponent you are fighting against.

			if ( encounter.equalsIgnoreCase( "Animated Nightstand" ) )
			{
				encounter = responseText.indexOf( "darkstand.gif" ) != -1 ?
					"Animated Nightstand (Mahogany)" : "Animated Nightstand (White)" ;
			}
			else if ( encounter.equalsIgnoreCase( "Orcish Frat Boy" ) )
			{
				encounter =
					responseText.indexOf( "fratskirt.gif" ) != -1 ? "Orcish Frat Boy (Pledge)" :
					responseText.indexOf( "rectify" ) != -1 ? "Orcish Frat Boy (Music Lover)" :
						"Orcish Frat Boy (Paddler)";
			}
			else if ( encounter.equalsIgnoreCase( "Trippy Floating Head" ) )
			{
				encounter =
					responseText.indexOf( "kasemhead.gif" ) != -1 ? "Trippy Floating Head (Casey Kasem)" :
					responseText.indexOf( "tarkinhead.gif" ) != -1 ? "Trippy Floating Head (Grand Moff Tarkin)" :
						"Trippy Floating Head (Mona Lisa)";
			}
			else if ( encounter.equalsIgnoreCase( "Ninja Snowman" ) )
			{
				encounter = responseText.indexOf( "ninjarice.gif" ) != -1 ?
					"Ninja Snowman (Chopsticks)" : "Ninja Snowman (Hilt/Mask)";
			}
			else if ( encounter.equalsIgnoreCase( "Ancient Protector Spirit" ) )
			{
				HiddenCityRequest.addHiddenCityLocation( 'P' );

			}
			else if ( encounter.equalsIgnoreCase( "The Darkness" ) &&
				responseText.indexOf( "darkness.gif" ) != -1 )
			{
				encounter = "The Darkness (blind)";
			}

			else if ( encounter.equalsIgnoreCase( "giant octopus" ) )
			{
				if ( KoLConstants.inventory.contains( ItemPool.get( ItemPool.GRAPPLING_HOOK, 1 ) ) )
				{
					ResultProcessor.processItem( ItemPool.GRAPPLING_HOOK, -1 );
				}
			}

			FightRequest.encounterLookup = CustomCombatManager.encounterKey( encounter );
			FightRequest.monsterData = MonsterDatabase.findMonster( FightRequest.encounterLookup, false );
			if ( FightRequest.monsterData == null &&
				EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() == ItemPool.SWORD_PREPOSITIONS )
			{
				FightRequest.encounterLookup =
					StringUtilities.lookupPrepositions( FightRequest.encounterLookup );
				FightRequest.monsterData = MonsterDatabase.findMonster(
					FightRequest.encounterLookup, false );
			}

			FightRequest.isTrackingFights = false;
			FightRequest.waitingForSpecial = false;
			for ( int i = 0; i < 10; ++i )
			{
				if ( CustomCombatManager.getShortCombatOptionName(
					CustomCombatManager.getSetting(
						FightRequest.encounterLookup, i ) ).equals( "special" ) )
				{
					FightRequest.waitingForSpecial = true;
					break;
				}
			}

			autoAttacked = FightRequest.checkForInitiative( responseText );
		}
		else
		{
			// Otherwise, the player can change the monster
			String newMonster = null;

			m = CLEESH_PATTERN.matcher( responseText );
			if ( m.find() )
			{
				newMonster = m.group(1);
			}

			// You start to run up to the hole, then change your
			// mind as a giant sandworm erupts out of it, howling
			// with fury.

			else if ( responseText.indexOf( "a giant sandworm erupts out of it" ) != -1 )
			{
				newMonster = "giant sandworm";
			}

			if ( newMonster != null )
			{
				FightRequest.encounterLookup = CustomCombatManager.encounterKey( newMonster );
				FightRequest.monsterData = MonsterDatabase.findMonster( FightRequest.encounterLookup, false );
				FightRequest.healthModifier = 0;
			}
		}

		// Preprocess results and register new items
		ResultProcessor.registerNewItems( responseText );

		// Assume this response does not warrant a refresh
		FightRequest.shouldRefresh = false;

		// Determine whether entire response is in haiku
		FightRequest.haveHaikuResults( responseText );

		// If we have haiku results, process everything - monster
		// health, familiar actions, dropped items, stat gains, ...
		if ( FightRequest.haveHaikuResults )
		{
			// Parse haiku and process everything in order
			FightRequest.processHaikuResults( responseText );
		}
		else
		{
			// Experimental: clean HTML and process it
			FightRequest.processNormalResults( responseText );
		}

		// Look for special effects
		FightRequest.updateMonsterHealth( responseText, 0 );

		// *** This doesn't seem right, but is currently necessary for
		// *** CCS scripts to behave correctly. FIX
		if ( autoAttacked )
		{
			++FightRequest.preparatoryRounds;
			++FightRequest.currentRound;
		}

		// Check for equipment breakage.

		if ( responseText.indexOf( "Your antique helmet, weakened" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_HELMET,
				"Your antique helmet broke." );
		}

		if ( responseText.indexOf( "sunders your antique spear" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_SPEAR,
				"Your antique spear broke." );
		}

		if ( responseText.indexOf( "Your antique shield, weakened" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_SHIELD,
				"Your antique shield broke." );
		}

		if ( responseText.indexOf( "Your antique greaves, weakened" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.ANTIQUE_GREAVES,
				"Your antique greaves broke." );
		}

		// You try to unlock your cyber-mattock, but the battery's
		// dead.  Since the charger won't be invented for several
		// hundred years, you chuck the useless hunk of plastic as far
		// from you as you can.

		if ( responseText.indexOf( "You try to unlock your cyber-mattock" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.CYBER_MATTOCK,
				"Your cyber-mattock broke." );
		}

		// "You sigh and discard the belt in a nearby trash can."
		if ( responseText.indexOf( "You sigh and discard the belt" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.CHEAP_STUDDED_BELT,
				"Your cheap studded belt broke." );
		}

		if ( responseText.indexOf( "Your sugar chapeau slides" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_CHAPEAU,
				"Your sugar chapeau shattered." );
		}

		if ( responseText.indexOf( "your sugar shank handle" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHANK,
				"Your sugar shank shattered." );
		}

		if ( responseText.indexOf( "drop something as sticky as the sugar shield" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHIELD,
				"Your sugar shield shattered." );
		}

		if ( responseText.indexOf( "Your sugar shillelagh absorbs the shock" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHILLELAGH,
				"Your sugar shillelagh shattered." );
		}

		if ( responseText.indexOf( "Your sugar shirt falls apart" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHIRT,
				"Your sugar shirt shattered." );
		}

		if ( responseText.indexOf( "Your sugar shotgun falls apart" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHOTGUN,
				"Your sugar shotgun shattered." );
		}

		if ( responseText.indexOf( "Your sugar shorts crack" ) != -1 )
		{
			EquipmentManager.breakEquipment( ItemPool.SUGAR_SHORTS,
				"Your sugar shorts shattered." );
		}

		// "The Slime draws back and shudders, as if it's about to sneeze.
		// Then it blasts you with a massive loogie that sticks to your
		// rusty grave robbing shovel, pulls it off of you, and absorbs
		// it back into the mass."

		m = FightRequest.SLIMED_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			int id = ItemDatabase.getItemId( m.group( 1 ) );
			if ( id > 0 )
			{
				EquipmentManager.discardEquipment( id );
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your " +
					m.group( 1 ) + " got slimed." );
			}
		}

		// "As you're trying to get away, you sink in the silty muck on
		// the sea floor. You manage to get yourself unmired, but your
		// greaves seem to have gotten instantly rusty in the process..."
		if ( responseText.indexOf( "have gotten instantly rusty" ) != -1 )
		{
			EquipmentManager.discardEquipment( ItemPool.ANTIQUE_GREAVES );
			KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Your antique greaves got rusted." );
		}

		// Check for familiar item drops
		if ( responseText.indexOf( "too preoccupied" ) != -1 &&
			responseText.indexOf( "this world" ) != -1 )
		{
			Preferences.increment( "_gongDrops", 1 );
		}

		if ( responseText.indexOf( "He tosses you a bottle" ) != -1 &&
			responseText.indexOf( "absinthe" ) != -1)
		{
			Preferences.increment( "_absintheDrops", 1 );
		}

		if ( responseText.indexOf( "produces a rainbow-colored mushroom" ) != -1 )
		{
			Preferences.increment( "_astralDrops", 1 );
		}

		if ( responseText.indexOf( "belches some murky fluid back" ) != -1 )
		{
			Preferences.increment( "_aguaDrops", 1 );
		}

		if ( responseText.indexOf( "shimmers briefly, and you feel it getting earlier." ) != -1 )
		{
			Preferences.increment( "_riftletAdv", 1 );
		}

		if ( responseText.indexOf( "sees that you're about to get attacked and trips it before it can attack you." ) != -1
			|| responseText.indexOf( "does the Time Warp, then does the Time Warp again. Clearly, madness has taken its toll on him." ) != -1
			|| responseText.indexOf( "The air shimmers around you." ) != -1 )
		{
			Preferences.increment( "_timeHelmetAdv", 1 );
		}

		if ( responseText.indexOf( "into last week. It saves you some time, because you already beat" ) != -1 )
		{
			Preferences.increment( "_vmaskAdv", 1 );
		}

		int blindIndex = responseText.indexOf( "... something.</div>" );
		while ( blindIndex != -1 )
		{
			RequestLogger.printLine( "You acquire... something." );
			if ( Preferences.getBoolean( "logAcquiredItems" ) )
			{
				RequestLogger.updateSessionLog( "You acquire... something." );
			}

			blindIndex = responseText.indexOf( "... something.</div>", blindIndex + 1 );
		}

		switch ( KoLAdventure.lastAdventureId() )
		{
		case 182: // Barrel with Something Burning in it
		case 183: // Near an Abandoned Refrigerator
		case 184: // Over Where the Old Tires Are
		case 185: // Out by that Rusted-Out Car
			// Quest gremlins might have a tool.
			IslandDecorator.handleGremlin( responseText );
			break;

		case 132: // Battlefield (Frat Uniform)
		case 140: // Battlefield (Hippy Uniform)
			IslandDecorator.handleBattlefield( responseText );
			break;

		case 167: // Hobopolis Town Square
			HobopolisDecorator.handleTownSquare( responseText );
			break;
		}

		// Reset round information if the battle is complete.
		// This is recognized when fight.php has no data.

		if ( responseText.indexOf( Preferences.getBoolean( "serverAddsCustomCombat" ) ?
					   "(show old combat form)" :
					   "fight.php" ) != -1 )
		{
			FightRequest.foundNextRound = true;
			return;
		}

		// The turtle blinks at you with gratitude for freeing it from
		// its brainwashing, and trudges off over the horizon.
		// ...Eventually.
		if ( responseText.indexOf( "freeing it from its brainwashing" ) != -1 )
		{
			int free = Preferences.increment( "guardTurtlesFreed" );
			String message = "Freed guard turtle #" + free;
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		if ( responseText.indexOf( "your Epic Weapon reverts to its original form in a puff of failure" ) != -1 )
		{
			FightRequest.transmogrifyNemesisWeapon( true );
		}

		// Check for bounty item not dropping from a monster
		// that is known to drop the item.

		int bountyItemId = Preferences.getInteger( "currentBountyItem" );
		if ( monsterData != null && bountyItemId != 0 )
		{
			AdventureResult bountyItem = new AdventureResult( bountyItemId, 1 );
			String bountyItemName = bountyItem.getName();

			if ( monsterData.getItems().contains( bountyItem ) && responseText.indexOf( bountyItemName ) == -1 )
			{
				KoLmafia.updateDisplay( KoLConstants.PENDING_STATE, "Bounty item failed to drop from expected monster." );
			}
		}

		// Check for GMoB defeat
		if ( responseText.indexOf( "guy made of bee pollen" ) != -1 )
		{
			// Record that we beat the guy made of bees.
			Preferences.setBoolean( "guyMadeOfBeesDefeated", true );
		}

		// Check for runaways. Only a free runaway decreases chance
		if ( responseText.indexOf( "shimmers as you quickly float away" ) != -1 )
		{
			Preferences.increment( "_navelRunaways", 1 );
		}

		if ( responseText.indexOf( "his back, and flooms away" ) != -1 )
		{
			Preferences.increment( "_banderRunaways", 1 );
		}

		// Check for worn-out stickers
		int count = 0;
		m = WORN_STICKER_PATTERN.matcher( responseText );
		while ( m.find() )
		{
			++count;
		}
		if ( count > 0 )
		{
			KoLmafia.updateDisplay( (count == 1 ? "A sticker" : count + " stickers") +
				" fell off your weapon." );
			EquipmentManager.stickersExpired( count );
		}

		// Check for ballroom song hint
		m = BALLROOM_SONG_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			Preferences.setInteger( "lastQuartetAscension", KoLCharacter.getAscensions() );
			Preferences.setInteger( "lastQuartetRequest", m.start( 1 ) != -1 ? 1 :
				m.start( 2 ) != -1 ? 2 : 3 );
		}

		// Check for special familiar actions
		FamiliarData familiar = KoLCharacter.getFamiliar();
		switch ( familiar.getId() )
		{
		case FamiliarPool.HARE:
			// <name> pulls an oversized pocketwatch out of his
			// waistcoat and winds it. "Two days slow, that's what
			// it is," he says.
			if ( responseText.indexOf( "oversized pocketwatch" ) != -1 )
			{
				Preferences.increment( "extraRolloverAdventures", 1 );
				Preferences.increment( "_hareAdv", 1 );
			}

			// The dormouse emerges groggily from <names>'s
			// waistcoat and gives the watch another turn. He
			// vanishes back into the pocket with a sleepy 'feed
			// your head.'
			break;

		case FamiliarPool.GIBBERER:
			// <name> mutters dark secrets under his breath, and
			// you feel time slow down.
			if ( responseText.indexOf( "you feel time slow down" ) != -1 )
			{
				Preferences.increment( "extraRolloverAdventures", 1 );
				Preferences.increment( "_gibbererAdv", 1 );
			}
			break;

		case FamiliarPool.STOCKING_MIMIC:
			// <name> reaches deep inside himself and pulls out a
			// big bag of candy. Cool!
			if ( responseText.indexOf( "pulls out a big bag of candy" ) != -1 )
			{
				AdventureResult item = ItemPool.get( ItemPool.BAG_OF_MANY_CONFECTIONS, 1 );
				// The Stocking Mimic will do this once a day
				Preferences.setBoolean( "_bagOfCandy", true );
				// Add bag of many confections to inventory
				ResultProcessor.processItem( ItemPool.BAG_OF_MANY_CONFECTIONS, 1 );
				// Equip familiar with it
				familiar.setItem( item );
			}

			// <name> gorges himself on candy from his bag.
			if ( responseText.indexOf( "gorges himself on candy from his bag" ) != -1 )
			{
				familiar.addExperience( 1 );
			}
			break;
		}

		// Cancel any combat modifiers
		Modifiers.overrideModifier( "fightMods", null );

		if ( responseText.indexOf( "<!--WINWINWIN-->" ) != -1 )
		{
			String monster = FightRequest.encounterLookup;

			if ( monster.equalsIgnoreCase( "Black Pudding" ) )
			{
				Preferences.increment( "blackPuddingsDefeated", 1 );
			}
			else if ( monster.equalsIgnoreCase( "Ancient Protector Spirit" ) )
			{
				HiddenCityRequest.addHiddenCityLocation( 'D' );
			}
			else if ( monster.equalsIgnoreCase( "Wumpus" ) )
			{
				WumpusManager.reset();
			}
			else if ( !FightRequest.castCleesh &&
				Preferences.getString( "lastAdventure" ).equalsIgnoreCase(
					"A Maze of Sewer Tunnels" ) )
			{
				AdventureResult result = AdventureResult.tallyItem(
					"sewer tunnel explorations", false );
				AdventureResult.addResultToList( KoLConstants.tally, result );
			}

			// Give your summoned combat entity some experience
			if ( FightRequest.summonedGhost )
			{
				// The Angel Hair Wisp can leave the battle
				// before you win. We'll check if the summoned
				// entity is still present by looking for its
				// image.

				for ( int i = 0; i < KoLCharacter.COMBAT_ENTITIES.length; ++ i )
				{
					Object [] entity = KoLCharacter.COMBAT_ENTITIES[i];
					String gif = (String)entity[4];
					if ( responseText.indexOf( gif ) != -1 )
					{
						Preferences.increment( "pastamancerGhostExperience", 1 );
						break;
					}
				}
			}
		}

		// "You pull out your personal massager and use it to work the
		// kinks out of your neck and your back. You stop there,
		// though, as nothing below that point is feeling particularly
		// kinky. Unfortunately, it looks like the batteries in the
		// thing were only good for that one use."

		if ( responseText.indexOf( "You pull out your personal massager" ) != -1 )
		{
			ResultProcessor.processItem( ItemPool.PERSONAL_MASSAGER, -1 );
			KoLConstants.activeEffects.remove( KoLAdventure.BEATEN_UP );
		}

		FightRequest.foundNextRound = true;
		FightRequest.clearInstanceData();
	}

	private static final boolean getSpecialAction()
	{
		ArrayList items = new ArrayList();

		boolean haveSkill, haveItem;
		String pref = Preferences.getString( "autoOlfact" );
		if ( !pref.equals( "" ) && !KoLConstants.activeEffects.contains( EffectPool.get( EffectPool.ON_THE_TRAIL ) ) )
		{
			haveSkill = KoLCharacter.hasSkill( "Transcendent Olfaction" ) &&
				!KoLCharacter.inBadMoon() &&
				( Preferences.getBoolean( "autoManaRestore" ) || KoLCharacter.getCurrentMP() >= SkillDatabase.getMPConsumptionById( SkillDatabase.OLFACTION ) );
			haveItem = KoLConstants.inventory.contains( FightRequest.EXTRACTOR );
			if ( (haveSkill | haveItem) && shouldTag( pref, "autoOlfact triggered" ) )
			{
				if ( haveSkill )
				{
					FightRequest.action1 = OLFACTION_ACTION;
					return true;
				}
				items.add( String.valueOf( ItemPool.ODOR_EXTRACTOR ) );
			}
		}

		pref = Preferences.getString( "autoPutty" );
		if ( !pref.equals( "" ) )
		{
			haveItem = KoLConstants.inventory.contains( FightRequest.PUTTY_SHEET ) &&
				Preferences.getInteger( "spookyPuttyCopiesMade" ) < 5;
			boolean haveItem2 = KoLConstants.inventory.contains( FightRequest.CAMERA ) &&
				!KoLConstants.inventory.contains( FightRequest.SHAKING_CAMERA );
			if ( (haveItem || haveItem2) && shouldTag( pref, "autoPutty triggered" ) )
			{
				if (haveItem)
				{
					items.add( String.valueOf( ItemPool.SPOOKY_PUTTY_SHEET ) );
				}
				else
				{
					items.add( String.valueOf( ItemPool.CAMERA ) );
				}
			}
		}

		if ( Preferences.getBoolean( "autoSphereID" ) )
		{
			ItemPool.suggestIdentify( items, 2174, 2177, "lastStoneSphere" );
		}
		if ( Preferences.getBoolean( "autoPotionID" ) )
		{
			ItemPool.suggestIdentify( items, 819, 827, "lastBangPotion" );
		}

		switch ( items.size() )
		{
		case 0:
			return false;
		case 1:
			FightRequest.action1 = (String) items.get( 0 );
			return true;
		default:
			FightRequest.action1 = (String) items.get( 0 ) + "," +
				(String) items.get( 1 );
			return true;
		}
	}

	private static final boolean shouldTag( String pref, String msg )
	{
		boolean isAbort = false, isMonster = false, rv;
		List items = null;

		if ( pref.endsWith( " abort" ) )
		{
			isAbort = true;
			pref = pref.substring( 0, pref.length() - 6 ).trim();
		}

		if ( pref.equals( "goals" ) )
		{
			items = KoLConstants.conditions;
		}
		else if ( pref.startsWith( "monster " ) )
		{
			isMonster = true;
			pref = pref.substring( 8 ).trim();
		}
		else {
			if ( pref.startsWith( "item " ) )
			{
				pref = pref.substring( 5 );
			}
			Object[] temp = ItemFinder.getMatchingItemList(
				KoLConstants.inventory, pref );
			if ( temp == null )
			{
				return false;
			}
			items = Arrays.asList( temp );
		}

		if ( isMonster )
		{
			rv = FightRequest.encounterLookup.indexOf( pref ) != -1;
		}
		else if ( items.size() < 1 || FightRequest.monsterData == null )
		{
			rv = false;
		}
		else
		{
			rv = FightRequest.monsterData.getItems().containsAll( items );
		}

		if ( rv && isAbort )
		{
			KoLmafia.abortAfter( msg );
		}
		return rv;
	}
	
	private static final void transmogrifyNemesisWeapon( boolean reverse )
	{
		for ( int i = 0; i < FightRequest.NEMESIS_WEAPONS.length; ++i )
		{
			Object[] data = FightRequest.NEMESIS_WEAPONS[ i ];
			if ( KoLCharacter.getClassType() == data[ 0 ] )
			{
				EquipmentManager.transformEquipment( 
					(AdventureResult) data[ reverse ? 2 : 1 ],
					(AdventureResult) data[ reverse ? 1 : 2 ] );
				return;
			}
		}
	}

	private static final Pattern BANG_POTION_PATTERN =
		Pattern.compile( "You throw the (.*?) potion at your opponent.?.  It shatters against .*?[,\\.] (.*?)\\." );

	private static final void parseBangPotion( final String responseText )
	{
		Matcher bangMatcher = FightRequest.BANG_POTION_PATTERN.matcher( responseText );
		while ( bangMatcher.find() )
		{
			int potionId = ItemDatabase.getItemId( bangMatcher.group( 1 ) + " potion" );

			String effectText = bangMatcher.group( 2 );
			String[][] strings = ItemPool.bangPotionStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						potionId,
						819, 827,
						"lastBangPotion", " of " ) )
					{
						KoLmafia.updateDisplay( "All bang potions have been identified!" );
					}
					break;
				}
			}
		}
	}

	// You hold the rough stone sphere up in the air.
	private static final Pattern STONE_SPHERE_PATTERN =
		Pattern.compile( "You hold the (.*?) stone sphere up in the air.*?It radiates a (.*?)," );

	private static final void parseStoneSphere( final String responseText )
	{
		Matcher sphereMatcher = FightRequest.STONE_SPHERE_PATTERN.matcher( responseText );
		while ( sphereMatcher.find() )
		{
			int sphereId = ItemDatabase.getItemId( sphereMatcher.group( 1 ) + " stone sphere" );

			if ( sphereId == -1 )
			{
				continue;
			}

			String effectText = sphereMatcher.group( 2 );
			String[][] strings = ItemPool.stoneSphereStrings;

			for ( int i = 0; i < strings.length; ++i )
			{
				if ( effectText.indexOf( strings[i][1] ) != -1 )
				{
					if ( ItemPool.eliminationProcessor( strings, i,
						sphereId,
						2174, 2177,
						"lastStoneSphere", " of " ) )
					{
						KoLmafia.updateDisplay( "All stone spheres have been identified!" );
					}
					break;
				}
			}
		}
	}

	public static final String stoneSphereEffectToId( final String effect )
	{
		for ( int i = 2174; i <= 2177; ++i )
		{
			String itemId = String.valueOf( i );
			String value = Preferences.getString( "lastStoneSphere" + itemId );

			if ( value.equals( "plants" ) )
			{
				value = "nature";
			}

			if ( effect.equals( value ) )
			{
				return itemId;
			}
		}

		return null;
	}

	// The pirate sneers at you and replies &quot;<insult>&quot;

	private static final Pattern PIRATE_INSULT_PATTERN =
		Pattern.compile( "The pirate sneers \\w+ you and replies &quot;(.*?)&quot;" );

	// The first string is an insult you hear from Rickets.
	// The second string is the insult you must use in reply.

	private static final String [][] PIRATE_INSULTS =
	{
		{
			"Arrr, the power of me serve'll flay the skin from yer bones!",
			"Obviously neither your tongue nor your wit is sharp enough for the job."
		},
		{
			"Do ye hear that, ye craven blackguard?  It be the sound of yer doom!",
			"It can't be any worse than the smell of your breath!"
		},
		{
			"Suck on <i>this</i>, ye miserable, pestilent wretch!",
			"That reminds me, tell your wife and sister I had a lovely time last night."
		},
		{
			"The streets will run red with yer blood when I'm through with ye!",
			"I'd've thought yellow would be more your color."
		},
		{
			"Yer face is as foul as that of a drowned goat!",
			"I'm not really comfortable being compared to your girlfriend that way."
		},
		{
			"When I'm through with ye, ye'll be crying like a little girl!",
			"It's an honor to learn from such an expert in the field."
		},
		{
			"In all my years I've not seen a more loathsome worm than yerself!",
			"Amazing!  How do you manage to shave without using a mirror?"
		},
		{
			"Not a single man has faced me and lived to tell the tale!",
			"It only seems that way because you haven't learned to count to one."
		},
	};

	static {
		for ( int i = 0; i < PIRATE_INSULTS.length; ++i )
		{
			StringUtilities.registerPrepositions( PIRATE_INSULTS[ i ][ 0 ] );
			StringUtilities.registerPrepositions( PIRATE_INSULTS[ i ][ 1 ] );
		}
	}

	private static final void parsePirateInsult( final String responseText )
	{
		Matcher insultMatcher = FightRequest.PIRATE_INSULT_PATTERN.matcher( responseText );
		if ( insultMatcher.find() )
		{
			int insult = FightRequest.findPirateInsult( insultMatcher.group( 1 ) );
			if ( insult > 0 )
			{
				KoLCharacter.ensureUpdatedPirateInsults();
				if ( !Preferences.getBoolean( "lastPirateInsult" + insult ) )
				{	// it's a new one
					Preferences.setBoolean( "lastPirateInsult" + insult, true );
					AdventureResult result = AdventureResult.tallyItem( "pirate insult", false );
					AdventureResult.addResultToList( KoLConstants.tally, result );
					int count = FightRequest.countPirateInsults();
					float odds = FightRequest.pirateInsultOdds( count ) * 100.0f;
					RequestLogger.printLine( "Pirate insults known: " +
						count + " (" + KoLConstants.FLOAT_FORMAT.format( odds ) +
						"%)" );
				}
			}
		}
	}

	private static final int findPirateInsult( String insult )
	{
		if ( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() ==
			ItemPool.SWORD_PREPOSITIONS )
		{
			insult = StringUtilities.lookupPrepositions( insult );
		}
		for ( int i = 0; i < PIRATE_INSULTS.length; ++i )
		{
			if ( insult.equals( PIRATE_INSULTS[i][1] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final int findPirateRetort( String insult )
	{
		if ( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getItemId() ==
			ItemPool.SWORD_PREPOSITIONS )
		{
			insult = StringUtilities.lookupPrepositions( insult );
		}
		for ( int i = 0; i < PIRATE_INSULTS.length; ++i )
		{
			if ( insult.equals( PIRATE_INSULTS[i][0] ) )
			{
				return i + 1;
			}
		}
		return 0;
	}

	public static final String findPirateRetort( final int insult )
	{
		KoLCharacter.ensureUpdatedPirateInsults();
		if ( Preferences.getBoolean( "lastPirateInsult" + insult ) )
		{
			return PIRATE_INSULTS[insult - 1][1];
		}
		return null;
	}

	public static final int countPirateInsults()
	{
		KoLCharacter.ensureUpdatedPirateInsults();

		int count = 0;
		for ( int i = 1; i <= 8; ++i )
		{
			if ( Preferences.getBoolean( "lastPirateInsult" + i ) )
			{
				count += 1;
			}
		}

		return count;
	}

	public static final float pirateInsultOdds()
	{
		return FightRequest.pirateInsultOdds( FightRequest.countPirateInsults() );
	}

	public static final float pirateInsultOdds( int count )
	{
		// If you know less than three insults, you can't possibly win.
		if ( count < 3 )
		{
			return 0.0f;
		}

		// Otherwise, your probability of winning is:
		//   ( count ) / 8	the first contest
		//   ( count - 1 ) / 8	the second contest
		//   ( count - 2 ) / 6	the third contest

		float odds = 1.0f;

		odds *= ( count * 1.0f ) / 8;
		odds *= ( count * 1.0f - 1 ) / 7;
		odds *= ( count * 1.0f - 2 ) / 6;

		return odds;
	}

	private static final void parseGrubUsage( final String location, final String responseText )
	{
		if ( location.indexOf( "7074" ) == -1 )
		{
			return;
		}

		// You concentrate on one of the burrowgrubs digging its way
		// through your body, and absorb it into your bloodstream.
		// It's refreshingly disgusting!

		if ( responseText.indexOf( "refreshingly disgusting" ) != -1 )
		{
			// We have used our burrowgrub hive today
			Preferences.setBoolean( "burrowgrubHiveUsed", true );

			int uses = Preferences.getInteger( "burrowgrubSummonsRemaining" );

			// <option value="7074" picurl="nopic" selected>Consume
			// Burrowgrub (0 Mojo Points)</option>

			if ( responseText.indexOf( "option value=\"7074\"" ) == -1 )
			{
				// No more uses today
				uses = 0;
			}
			// At least one more use today
			else if ( uses >= 2)
			{
				uses = uses - 1;
			}
			else
			{
				uses = 1;
			}

			Preferences.setInteger( "burrowgrubSummonsRemaining", uses );
		}
	}

	private static final void parseFlyerUsage( final String location, final String responseText )
	{
		if ( location.indexOf( "240" ) == -1 )
		{	// jam band flyers=2404, rock band flyers=2405
			return;
		}

		// You slap a flyer up on your opponent. It enrages it.

		if ( responseText.indexOf( "You slap a flyer" ) != -1 )
		{
			int ML = Math.max( 0, FightRequest.getMonsterAttack() );
			Preferences.increment( "flyeredML", ML );
			AdventureResult result = AdventureResult.tallyItem(
				"Arena flyer ML", ML, false );
			AdventureResult.addResultToList( KoLConstants.tally, result );
		}
	}

	private static final void parseGhostSummoning( final String location, final String responseText )
	{
		if ( location.indexOf( "summon" ) == -1 )
		{
			return;
		}

		String name = null;
		String type = null;

		KoLCharacter.ensureUpdatedPastamancerGhost();
		for ( int i = 0; i < KoLCharacter.COMBAT_ENTITIES.length; ++ i )
		{
			Object [] entity = KoLCharacter.COMBAT_ENTITIES[i];
			Pattern pattern = (Pattern)entity[3];
			Matcher matcher = pattern.matcher( responseText );
			if ( matcher.find() )
			{
				name = matcher.group(1);
				type = (String)entity[0];
				break;
			}
		}

		if ( name == null )
		{
			return;
		}

		FightRequest.summonedGhost = true;

		if ( !name.equals( Preferences.getString( "pastamancerGhostName" ) ) ||
		     !type.equals( Preferences.getString( "pastamancerGhostType" ) ) )
		{
			Preferences.setString( "pastamancerGhostName", name );
			Preferences.setString( "pastamancerGhostType", type );
			Preferences.setInteger( "pastamancerGhostExperience", 0 );
		}

		int uses = Preferences.getInteger( "pastamancerGhostSummons" );
		int limit = KoLCharacter.hasEquipped( ItemPool.get( ItemPool.SPAGHETTI_BANDOLIER, 1 ) ) ? 10 : 15;

		// You are mentally exhausted by the effort of summoning <name>.
		if ( responseText.indexOf( "You are mentally exhausted" ) != -1 )
		{
			uses = limit;
		}

		// Your brain feels tired.
		else if ( responseText.indexOf( "Your brain feels tired" ) != -1 && uses < limit - 2 )
		{
			uses = limit - 2;
		}
		else
		{
			++uses;
		}

		Preferences.setInteger( "pastamancerGhostSummons", uses );
	}

	public static final void parseCombatItems( String responseText )
	{
		int startIndex = responseText.indexOf( "<select name=whichitem>" );
		if ( startIndex == -1 ) return;
		int endIndex = responseText.indexOf( "</select>", startIndex );
		if ( endIndex == -1 ) return;
		Matcher m = FightRequest.COMBATITEM_PATTERN.matcher(
			responseText.substring( startIndex, endIndex ) );
		while ( m.find() )
		{
			int itemId = StringUtilities.parseInt( m.group( 1 ) );
			if ( itemId <= 0 ) continue;
			int actualQty = StringUtilities.parseInt( m.group( 2 ) );
			AdventureResult ar = ItemPool.get( itemId, 1 );
			int currentQty = ar.getCount( KoLConstants.inventory );
			if ( actualQty != currentQty )
			{
				ar = ar.getInstance( actualQty - currentQty );
				ResultProcessor.processResult( ar );
				RequestLogger.updateSessionLog( "Adjusted combat item count: " + ar );
			}
		}
	}

	private static final void getRound( final StringBuffer action )
	{
		action.setLength( 0 );
		if ( FightRequest.currentRound == 0 )
		{
			action.append( "After Battle: " );
		}
		else
		{
			action.append( "Round " );
			action.append( FightRequest.currentRound );
			action.append( ": " );
		}
	}

	private static final void updateMonsterHealth( final String responseText, final int damageThisRound )
	{
		FightRequest.healthModifier += damageThisRound;

		if ( !Preferences.getBoolean( "logMonsterHealth" ) )
		{
			return;
		}

		// Done with all processing for monster damage, now handle responseText.
		StringBuffer action = new StringBuffer();

		FightRequest.logMonsterDamage( action, damageThisRound );

		// Boss Bat can muck with the monster's HP, but doesn't have
		// normal text.

		Matcher m = FightRequest.BOSSBAT_PATTERN.matcher( responseText );
		if ( m.find() )
		{
			int damage = -StringUtilities.parseInt( m.group( 1 ) );
			FightRequest.healthModifier += damage;
			action.append( FightRequest.encounterLookup );
			action.append( " sinks his fangs into you!" );
			FightRequest.logMonsterDamage( action, damage );
		}

		// Even though we don't have an exact value, at least try to
		// detect if the monster's HP has changed.  Once spaded, we can
		// insert some minimal/maximal values here.

		if ( FightRequest.GHUOL_HEAL.matcher( responseText ).find() || FightRequest.NS_HEAL.matcher( responseText ).find() )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " heals an unspaded amount of hit points." );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		Matcher detectiveMatcher = FightRequest.DETECTIVE_PATTERN.matcher( responseText );
		if ( detectiveMatcher.find() )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " shows detective skull health estimate of " );
			action.append( detectiveMatcher.group( 1 ) );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		Matcher helmetMatcher = FightRequest.SPACE_HELMET_PATTERN.matcher( responseText );
		if ( helmetMatcher.find() )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " shows toy space helmet health estimate of " );
			action.append( helmetMatcher.group( 1 ) );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int hp = DwarfFactoryRequest.deduceHP( responseText );
		if ( hp > 0  )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " shows dwarvish war mattock health estimate of " );
			action.append( hp );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int attack = DwarfFactoryRequest.deduceAttack( responseText );
		if ( attack > 0 )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " shows dwarvish war helmet attack rating of " );
			action.append( attack );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		int defense = DwarfFactoryRequest.deduceDefense( responseText );
		if ( defense > 0 )
		{
			FightRequest.getRound( action );
			action.append( FightRequest.encounterLookup );
			action.append( " shows dwarvish war kilt defense rating of " );
			action.append( defense );

			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}
	}

	private static final void logMonsterDamage( final StringBuffer action, final int damage )
	{
		if ( damage == 0 )
		{
			return;
		}

		FightRequest.getRound( action );
		action.append( FightRequest.encounterLookup );

		if ( damage > 0 )
		{
			action.append( " takes " );
			action.append( damage );
			action.append( " damage." );
		}
		else
		{
			action.append( " heals " );
			action.append( -1 * damage );
			action.append( " hit points." );
		}

		String message = action.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	// NOTE: All of the non-empty patterns that can match in the first group
	// imply that the entire expression should be ignored.	If you add one
	// and this is not the case, then correct the use of this Pattern below.

	private static final Pattern PHYSICAL_PATTERN =
		Pattern.compile( "(your blood, to the tune of|stabs you for|sown|You lose|You gain|strain your neck|approximately|) #?(\\d[\\d,]*) (\\([^.]*\\) |)((?:[^\\s]+ ){0,3})(?:damage|points?|notch(?:es)?|to your opponent|force damage|tiny holes)" );

	private static final Pattern ELEMENTAL_PATTERN =
		Pattern.compile( "(sown|) \\+?([\\d,]+) (\\([^.]*\\) |)(?:(?:slimy, (?:clammy|gross) |hotsy-totsy |)damage|points|HP worth)" );

	private static final Pattern SECONDARY_PATTERN = Pattern.compile( "\\+([\\d,]+)" );

	private static final int parseNormalDamage( final String text )
	{
		if ( text.equals( "" ) )
		{
			return 0;
		}

		int damage = 0;

		Matcher m = FightRequest.ELEMENTAL_PATTERN.matcher( text );
		if ( m.find() )
		{
			if ( !m.group( 1 ).equals( "" ) )
			{
				return 0;
			}

			damage += StringUtilities.parseInt( m.group( 2 ) );

			Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher( m.group( 3 ) );
			while ( secondaryMatcher.find() )
			{
				damage += StringUtilities.parseInt( secondaryMatcher.group( 1 ) );
			}
			return damage;
		}

		m = FightRequest.PHYSICAL_PATTERN.matcher( text );
		if ( m.find() )
		{
			// Currently, all of the explicit attack messages that
			// preceed the number all imply that this is not damage
			// against the monster or is damage that should not
			// count (reap/sow X damage.)

			if ( !m.group( 1 ).equals( "" ) )
			{
				return 0;
			}

			// "shambles up to your opponent" following a number is
			// most likely a familiar naming problem, so it should
			// not count.

			if ( m.group( 4 ).equals( "shambles up " ) )
			{
				return 0;
			}

			damage += StringUtilities.parseInt( m.group( 2 ) );

			// The last string contains all of the extra damage
			// from dual-wielding or elemental damage, e.g. "(+3)
			// (+10)".

			Matcher secondaryMatcher = FightRequest.SECONDARY_PATTERN.matcher( m.group( 3 ) );
			while ( secondaryMatcher.find() )
			{
				damage += StringUtilities.parseInt( secondaryMatcher.group( 1 ) );
			}

			return damage;
		}

		return 0;
	}

	private static final Pattern HAIKU_DAMAGE2_PATTERN =
		Pattern.compile( "<b>(?:<font color=[\"]?(\\w+)[\"]?>)?([\\d,]+)(?:</font>)?</b> damage" );

	public static Pattern HAIKU_PATTERN = Pattern.compile( "<[tT]able[^>]*><tr>(?:<td[^>]*><img[^>]*/([^/]*\\.gif)(?:[^>]*descitem\\(([\\d]*)\\))?[^>]*></td>)?(?:<td[^>]*>)?(?:<span[^>]*title=\"([^\"]*)\"[^>]*>)?(?:<[tT]able><tr>)?<td valign=center[^>]*>(.*?)</td>(?:</tr></table>(?:</span>)?</td>)?</tr></table>" );

	private static Pattern INT_PATTERN = Pattern.compile( "([\\d]+)" );

	public static Matcher getHaikuMatcher( String text )
	{
		return FightRequest.HAIKU_PATTERN.matcher( text );
	}

	private static final void processHaikuResults( final String text )
	{
		Matcher matcher = FightRequest.getHaikuMatcher( text );
		if ( !matcher.find() )
		{
			return;
		}

		String familiar = KoLCharacter.getFamiliar().getImageLocation();
		boolean logFamiliar = Preferences.getBoolean( "logFamiliarActions" );
		boolean logMonsterHealth = Preferences.getBoolean( "logMonsterHealth" );
		StringBuffer action = new StringBuffer();

		boolean shouldRefresh = false;
		boolean nunnery = KoLAdventure.lastVisitedLocation().getAdventureId().equals( "126" );
		boolean won = false;

		do
		{
			String image = matcher.group(1);

			if ( image == null )
			{
				// Damage from skills and spells comes without an image
				Matcher damageMatcher = FightRequest.HAIKU_DAMAGE2_PATTERN.matcher( matcher.group(0) );
				if ( damageMatcher.find() )
				{
					int damage = StringUtilities.parseInt( damageMatcher.group(2) );
					if ( logMonsterHealth )
					{
						FightRequest.logMonsterDamage( action, damage );
					}
					FightRequest.healthModifier += damage;
				}

				continue;
			}

			if ( image.equals( "happy.gif" ) )
			{
				won = true;
				continue;
			}

			String descid = matcher.group(2);

			if ( descid != null )
			{
				// Found an item
				int itemId = ItemDatabase.getItemIdFromDescription( descid );
				AdventureResult result = ItemPool.get( itemId, 1 );
				ResultProcessor.processItem( true, "You acquire an item:", result, (List) null );
				continue;
			}

			String haiku = matcher.group(4);

			if ( image.equals( familiar ) )
			{
				if ( logFamiliar )
				{
					String famact = StringUtilities.globalStringReplace( haiku, "<br>", " / " );
					famact = KoLConstants.ANYTAG_PATTERN.matcher( famact ).replaceAll( "" );
					FightRequest.getRound( action );
					action.append( famact );

					String message = action.toString();
					RequestLogger.printLine( message );
					RequestLogger.updateSessionLog( message );
				}

				ResultProcessor.processFamiliarWeightGain( haiku );
			}

			if ( FightRequest.foundHaikuDamage( matcher.group( 0 ), action, logMonsterHealth ) )
			{
				continue;
			}

			// Damage can come in other ways, too...
			if ( haiku.indexOf( "17 small cuts" ) != -1 )
			{
				// Casting The 17 Cuts
				if ( logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, 17 );
				}
				FightRequest.healthModifier += 17;
				continue;
			}

			Matcher m = INT_PATTERN.matcher( haiku );
			if ( !m.find() )
			{
				if ( image.equals( "strboost.gif" ) && haiku.indexOf( "<b>" ) != -1 )
				{
					String message = "You gain a Muscle point!";
					shouldRefresh |= ResultProcessor.processGainLoss( message, null );
					continue;
				}

				if ( image.equals( "snowflakes.gif" ) && haiku.indexOf( "<b>" ) != -1 )
				{
					String message = "You gain a Mysticality point!";
					shouldRefresh |= ResultProcessor.processGainLoss( message, null );
					continue;
				}

				if ( image.equals( "wink.gif" ) && haiku.indexOf( "<b>" ) != -1 )
				{
					String message = "You gain a Moxie point!";
					shouldRefresh |= ResultProcessor.processGainLoss( message, null );
					continue;
				}
				continue;
			}

			String points = m.group(1);

			if ( image.equals( "meat.gif" ) )
			{
				String message = "You gain " + points + " Meat";
				ResultProcessor.processMeat( message, won && nunnery );
				shouldRefresh = true;
				continue;
			}

			if ( image.equals( "hp.gif" ) )
			{
				// Gained or lost HP

				String gain = "lose";

				// Your wounds fly away
				// on a refreshing spring breeze.
				// You gain <b>X</b> hit points.

				// When <b><font color=black>XX</font></b> hit points<
				// are restored to your body,
				// you make an "ahhhhhhhh" sound.

				// You're feeling better --
				// <b><font color=black>XXX</font></b> hit points better -
				// than you were before.

				if ( haiku.indexOf( "Your wounds fly away" ) != -1 ||
				     haiku.indexOf( "restored to your body" ) != -1 ||
				     haiku.indexOf( "You're feeling better" ) != -1 )
				{
					gain = "gain";
				}

				String message = "You " + gain + " " + points + " hit points";
				shouldRefresh |= ResultProcessor.processGainLoss( message, null );
				continue;
			}

			if ( image.equals( "mp.gif" ) )
			{
				// Gained or lost MP

				String gain = "lose";

				// You feel quite refreshed,
				// like a frog drinking soda,
				// gaining <b>X</b> MP.

				// A contented belch.
				// Ripples in a mystic pond.
				// You gain <b>X</b> MP.

				// Like that wimp Dexter,
				// you have become ENERGIZED,
				// with <b>XX</b> MP.

				// <b>XX</b> magic points
				// fall upon you like spring rain.
				// Mana from heaven.

				// Spring rain falls within
				// metaphorically, I mean.
				// <b>XXX</b> mp.

				// <b>XXX</b> MP.
				// Like that sports drink commercial --
				// is it in you?  Yes.

				if ( haiku.indexOf( "You feel quite refreshed" ) != -1 ||
				     haiku.indexOf( "A contented belch" ) != -1 ||
				     haiku.indexOf( "ENERGIZED" ) != -1 ||
				     haiku.indexOf( "Mana from heaven" ) != -1 ||
				     haiku.indexOf( "Spring rain falls within" ) != -1 ||
				     haiku.indexOf( "sports drink" ) != -1 )
				{
					gain = "gain";
				}
				String message = "You " + gain + " " + points + " Mojo points";

				shouldRefresh |= ResultProcessor.processGainLoss( message, null );
				continue;
			}

			if ( image.equals( "strboost.gif" ) )
			{
				String message = "You gain " + points + " Strongness";
				shouldRefresh |= ResultProcessor.processStatGain( message, null );
				continue;
			}

			if ( image.equals( "snowflakes.gif" ) )
			{
				String message = "You gain " + points + " Magicalness";
				shouldRefresh |= ResultProcessor.processStatGain( message, null );
				continue;
			}

			if ( image.equals( "wink.gif" ) )
			{
				String message = "You gain " + points + " Roguishness";
				shouldRefresh |= ResultProcessor.processStatGain( message, null );
				continue;
			}

			if ( haiku.indexOf( "damage" ) != -1 )
			{
				// Using a combat item
				int damage = StringUtilities.parseInt( points );
				if ( logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, damage );
				}
				FightRequest.healthModifier += damage;
				continue;
			}
		} while ( matcher.find() );

		FightRequest.shouldRefresh = shouldRefresh;
	}

	private static final Pattern HAIKU_DAMAGE1_PATTERN =
		Pattern.compile( "title=\"Damage: ([^\"]+)\"" );

	private static final boolean foundHaikuDamage( final String text, final StringBuffer action, final boolean logMonsterHealth )
	{
		// Look for Damage: title in the image
		Matcher matcher = FightRequest.HAIKU_DAMAGE1_PATTERN.matcher( text );
		boolean foundDamageTitle = false;
		while ( matcher.find() )
		{
			foundDamageTitle = true;
			String[] pieces = matcher.group( 1 ).split( "[^\\d,]+" );
			int damage = 0;
			for ( int i = 0; i < pieces.length; ++i )
			{
				damage += StringUtilities.parseInt( pieces[ i ] );
			}
			if ( damage != 0 )
			{
				if ( logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, damage );
				}
				FightRequest.healthModifier += damage;
			}
		}

		return foundDamageTitle;
	}

	public static class TagStatus
	{
		public final String familiar;
		public final String diceMessage;
		public final String ghost;
		public final boolean logFamiliar;
		public final boolean logMonsterHealth;
		public final StringBuffer action;
		public boolean shouldRefresh;
		public boolean famaction = false;
		public boolean mosquito = false;
		public boolean dice = false;
		public boolean nunnery = false;
		public boolean won = false;

		public TagStatus()
		{
			FamiliarData current = KoLCharacter.getFamiliar();
			this.familiar = current.getImageLocation();
			this.diceMessage = ( current.getId() == FamiliarPool.DICE ) ? ( current.getName() + " begins to roll." ) : null;
			this.logFamiliar = Preferences.getBoolean( "logFamiliarActions" );
			this.logMonsterHealth = Preferences.getBoolean( "logMonsterHealth" );
			this.action = new StringBuffer();

			this.shouldRefresh = false;

			// Note if we are fighting The Themthar Hills
			KoLAdventure location = KoLAdventure.lastVisitedLocation();
			this.nunnery = location != null && location.getAdventureId().equals( "126" );

			if ( KoLCharacter.getClassType() == KoLCharacter.PASTAMANCER )
			{
				String name = Preferences.getString( "pastamancerGhostName" );
				this.ghost = name.equals( "" ) ? null : name;
			}
			else
			{
				this.ghost = null;
			}
		}
	}

	private static final void processNormalResults( final String text )
	{
		TagStatus status = new TagStatus();

		TagNode node = null;
		try
		{
			// Clean the HTML on this fight response page
			node = cleaner.clean( text );
		}
		catch ( IOException e )
		{
			// Oops.
			StaticEntity.printStackTrace( e );
			return;
		}

		if ( node == null )
		{
			RequestLogger.printLine( "HTML cleaning failed." );
			return;
		}

		// Find the 'monpic' image
		TagNode img = node.findElementByAttValue( "id", "monname", true, false );
		if ( img == null )
		{
			RequestLogger.printLine( "Cannot find monster." );
			return;
		}

		// Walk up the tree: <td><center><table><tbody><tr><td><img>
		//
		// The children of that node have everything interesting about
		// the fight.
		TagNode fight = img.getParent().getParent().getParent().getParent().getParent().getParent();

		if ( RequestLogger.isDebugging() && Preferences.getBoolean( "logCleanedHTML" ) )
		{
			FightRequest.logHTML( fight );
		}

		FightRequest.processNode( fight, status );

		FightRequest.shouldRefresh = status.shouldRefresh;
	}

	private static Pattern FUMBLE_PATTERN =
		Pattern.compile( "You drop your .*? on your .*?, doing ([\\d,]+) damage" );
	private static final Pattern MOSQUITO_PATTERN =
		Pattern.compile( "sucks some blood out of your opponent and injects it into you." );
	private static Pattern STABBAT_PATTERN = Pattern.compile( " stabs you for ([\\d,]+) damage" );
	private static Pattern CARBS_PATTERN = Pattern.compile( "some of your blood, to the tune of ([\\d,]+) damage" );

	private static final int parseFamiliarDamage( final String text, TagStatus status )
	{
		int damage = FightRequest.parseNormalDamage( text );

		// Mosquito can muck with the monster's HP, but doesn't have
		// normal text.

		switch ( KoLCharacter.getFamiliar().getId() )
		{
		case FamiliarPool.MOSQUITO:
		{
			Matcher m = FightRequest.MOSQUITO_PATTERN.matcher( text );
			if ( m.find() )
			{
				status.mosquito = true;
			}
			break;
		}

		case FamiliarPool.STAB_BAT:
		{
			Matcher m = FightRequest.STABBAT_PATTERN.matcher( text );

			if ( m.find() )
			{
				String message = "You lose " + m.group( 1 ) + " hit points";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			break;
		}

		case FamiliarPool.ORB:
		{
			Matcher m = FightRequest.CARBS_PATTERN.matcher( text );

			if ( m.find() )
			{
				String message = "You lose " + m.group( 1 ) + " hit points";
				status.shouldRefresh |= ResultProcessor.processGainLoss( message, null );
			}
			break;
		}
		}

		return damage;
	}

	private static final void processNode( final TagNode node, final TagStatus status )
	{
		String name = node.getName();
		StringBuffer action = status.action;

		// Skip scripts, forms, buttons, and html links
		if ( name.equals( "script" ) ||
		     name.equals( "form" ) ||
		     name.equals( "input" ) ||
		     name.equals( "a" ) ||
		     name.equals( "div" ) )
		{
			return;
		}

		/// node-specific processing
		if ( name.equals( "table" ) )
		{
			// Items have "rel" strings.
			String cl = node.getAttributeByName( "class" );
			String rel = node.getAttributeByName( "rel" );
			if ( cl != null && cl.equals( "item" ) && rel != null )
			{
				AdventureResult result = ItemDatabase.itemFromRelString( rel );
				ResultProcessor.processItem( true, "You acquire an item:", result, (List) null );
				return;
			}

			StringBuffer text = node.getText();
			String str = text.toString();

			if ( status.famaction )
			{
				status.famaction = false;
				if ( ResultProcessor.processFamiliarWeightGain( str ) )
				{
					return;
				}
				if ( status.logFamiliar )
				{
					FightRequest.logText( text, status );
				}
				int damage = FightRequest.parseFamiliarDamage( str, status );
				if ( damage != 0 )
				{
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterDamage( action, damage );
					}
					FightRequest.healthModifier += damage;
				}
				return;
			}

			// Tables often appear in fight results to hold images.
			TagNode inode = node.findElementByName( "img", true );
			if ( inode == null )
			{
				// No image. Parse combat damage.
				int damage = FightRequest.parseNormalDamage( str );
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, damage );
				}
				FightRequest.healthModifier += damage;
				return;
			}

			// Look for items and effects first
			String onclick = inode.getAttributeByName( "onclick" );
			if ( onclick != null )
			{
				if ( onclick.startsWith( "descitem" ) )
				{
					Matcher m = INT_PATTERN.matcher( onclick );
					if ( !m.find() )
					{
						return;
					}

					int itemId = ItemDatabase.getItemIdFromDescription( m.group(1) );
					AdventureResult result = ItemPool.get( itemId, 1 );
					ResultProcessor.processItem( true, "You acquire an item:", result, (List) null );
					return;
				}

				if ( onclick.startsWith( "eff" ) )
				{
					// Gain/loss of effect
					String effect = inode.getAttributeByName( "title" );
					// For prettiness
					String munged = StringUtilities.singleStringReplace( str, "(", " (" );
					ResultProcessor.processEffect( effect, munged );
					return;
				}
			}

			String src = inode.getAttributeByName( "src" );
			String image = src == null ? null : src.substring( src.lastIndexOf( "/" ) + 1 );

			if ( image.equals( "meat.gif" ) )
			{
				// Adjust for Can Has Cyborger
				str = StringUtilities.singleStringReplace( str, "gets", "gain" );
				str = StringUtilities.singleStringReplace( str, "Meets", "Meat" );

				// Adjust for The Sea
				str = StringUtilities.singleStringReplace( str, "manage to grab", "gain" );

				// If we are in The Themthar Hills and we have
				// seen the "you won" comment, the nuns take
				// the meat.

				ResultProcessor.processMeat( str, status.won && status.nunnery );
				status.shouldRefresh = true;
				return;
			}

			if ( image.equals( "hp.gif" ) ||
			     image.equals( "mp.gif" ) )
			{
				// You gain HP or MP
				if ( status.mosquito )
				{
					status.mosquito = false;
					Matcher m = INT_PATTERN.matcher( str );
					int damage = m.find() ? StringUtilities.parseInt( m.group(1) ) : 0;
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterDamage( action, damage );
					}
					FightRequest.healthModifier += damage;
				}

				status.shouldRefresh = ResultProcessor.processGainLoss( str, null );
				return;
			}

			if ( image.equals( status.familiar ) )
			{
				if ( ResultProcessor.processFamiliarWeightGain( str ) )
				{
					return;
				}

				// Familiar combat action?
				if ( status.logFamiliar )
				{
					FightRequest.logText( text, status );
				}

				int damage = FightRequest.parseFamiliarDamage( str, status );
				if ( damage != 0 )
				{
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterDamage( action, damage );
					}
					FightRequest.healthModifier += damage;
				}
				return;
			}

			if ( image.equals( "hkatana.gif" ) )
			{
				// You struck with your haiku katana. Pull the
				// damage out of the img tag if we can
				String title = inode.getAttributeByName( "title" );
				if ( title != null )
				{
					title = "title=\"" + title + "\"";
					if (foundHaikuDamage( title, action, status.logMonsterHealth ) )
					{

						return;
					}
				}
			}

			if ( image.equals( "realdolphin_r.gif" ) )
			{
				// You are slowed too much by the water, and a
				// stupid dolphin swims up and snags a seaweed
				// before you can grab it.

				// Inside this table is another table with
				// another image of the stolen dolphin item.

				TagNode tnode = node.findElementByName( "table", true );
				if ( tnode == null )
				{
					return;
				}

				TagNode inode2 = tnode.findElementByName( "img", true );
				if ( inode2 == null )
				{
					return;
				}

				String onclick2 = inode2.getAttributeByName( "onclick" );
				if ( onclick2 == null || !onclick2.startsWith( "descitem" ) )
				{
					return;
				}

				Matcher m = INT_PATTERN.matcher( onclick2 );
				String descid = m.find() ? m.group(1) : null;

				if ( descid == null )
				{
					return;
				}

				int itemId = ItemDatabase.getItemIdFromDescription( descid );
				if ( itemId == -1 )
				{
					return;
				}

				AdventureResult result = ItemPool.get( itemId, 1 );
				RequestLogger.printLine( "A dolphin stole: " + result );
				Preferences.setString( "dolphinItem", result.getName() );
				return;
			}

			// Combat item usage
			int damage = FightRequest.parseNormalDamage( str );
			if ( damage != 0 )
			{
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, damage );
				}
				FightRequest.healthModifier += damage;
				return;
			}

			// Unknown.
		}

		if ( name.equals( "p" ) )
		{
			StringBuffer text = node.getText();
			String str = text.toString();

			if ( FightRequest.processFumble( str, status ) )
			{
				return;
			}

			int damage = FightRequest.parseNormalDamage( str );
			if ( damage != 0 )
			{
				if ( status.logMonsterHealth )
				{
					FightRequest.logMonsterDamage( action, damage );
				}
				FightRequest.healthModifier += damage;
				return;
			}
		}

		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentToken )
			{
				CommentToken object = (CommentToken) child;
				String content = object.getContent();
				if ( content.equals( "familiarmessage" ) )
				{
					status.famaction = true;
				}
				else if ( content.equals( "WINWINWIN" ) )
				{
					status.won = true;
					FightRequest.currentRound = 0;
				}
				continue;
			}

			if ( child instanceof ContentToken )
			{
				ContentToken object = (ContentToken) child;
				String text = object.getContent().trim();

				if ( text.equals( "" ) )
				{
					continue;
				}

				if ( FightRequest.handleFuzzyDice( text, status ) )
				{
					return;
				}

				if ( FightRequest.processFumble( text, status ) )
				{
					return;
				}

				if ( text.indexOf( "you feel all warm and fuzzy" ) != -1 )
				{
					if ( status.logFamiliar )
					{
						FightRequest.logText( "A freed guard turtle returns.", status );
					}
					return;
				}

				if ( status.ghost != null && text.indexOf( status.ghost) != -1 )
				{
					// Pastamancer ghost action
					if ( status.logFamiliar )
					{
						FightRequest.logText( text, status );
					}
				}

				int damage = FightRequest.parseNormalDamage( text );
				if ( damage != 0 )
				{
					if ( status.logMonsterHealth )
					{
						FightRequest.logMonsterDamage( action, damage );
					}
					FightRequest.healthModifier += damage;
					return;
				}

				if ( text.startsWith( "You acquire a skill" ) )
				{
					TagNode bnode = node.findElementByName( "b", true );
					if ( bnode != null )
					{
						String skill = bnode.getText().toString();
						StaticEntity.learnSkill( skill );
					}
					continue;
				}

				if ( text.startsWith( "You gain" ) )
				{
					status.shouldRefresh = ResultProcessor.processGainLoss( text, null );
					continue;
				}

				if ( text.startsWith( "You can has" ) )
				{
					// Adjust for Can Has Cyborger
					text = StringUtilities.singleStringReplace( text, "can has", "gain" );
					ResultProcessor.processGainLoss( text, null );
					continue;
				}
				continue;
			}

			if ( child instanceof TagNode )
			{
				TagNode object = (TagNode) child;
				FightRequest.processNode( object, status );
				continue;
			}
		}
	}

	private static boolean handleFuzzyDice( String content, TagStatus status )
	{
		if ( !status.logFamiliar || status.diceMessage == null )
		{
			return false;
		}

		if ( content.startsWith( status.diceMessage ) )
		{
			status.dice = true;
			return true;
		}

		if ( !status.dice )
		{
			return false;
		}

		if ( content.equals( "&nbsp;&nbsp;&nbsp;&nbsp;" ) )
		{
			return true;
		}


		// We finally have the whole message.
		StringBuffer action = status.action;
		action.setLength( 0 );
		action.append( status.diceMessage );
		action.append( " " );
		action.append( " " );
		action.append( content );
		FightRequest.logText( action, status );

		// No longer accumulating fuzzy dice message
		status.dice = false;

		return true;
	}

	private static boolean processFumble( String text, TagStatus status )
	{
		Matcher m = FightRequest.FUMBLE_PATTERN.matcher( text );

		if ( m.find() )
		{
			String message = "You lose " + m.group( 1 ) + " hit points";
			status.shouldRefresh = ResultProcessor.processGainLoss( message, null );
			return true;
		}

		return false;
	}

	private static final void logText( StringBuffer buffer, final TagStatus status )
	{
		FightRequest.logText( buffer.toString(), status );
	}

	private static final void logText( String text, final TagStatus status )
	{
		text = StringUtilities.globalStringReplace( text, "<br>", " / " );
		text = KoLConstants.ANYTAG_PATTERN.matcher( text ).replaceAll( "" );

		StringBuffer action = status.action;
		FightRequest.getRound( action );
		action.append( text );
		String message = action.toString();
		RequestLogger.printLine( message );
		RequestLogger.updateSessionLog( message );
	}

	private static final void clearInstanceData()
	{
		IslandDecorator.startFight();
		FightRequest.castNoodles = false;
		FightRequest.castCleesh = false;
		FightRequest.canOlfact = true;
		FightRequest.jiggledChefstaff = false;
		FightRequest.stealthMistletoe = 1;
		FightRequest.summonedGhost = false;
		FightRequest.desiredScroll = null;

		FightRequest.levelModifier = 0;
		FightRequest.healthModifier = 0;

		FightRequest.action1 = null;
		FightRequest.action2 = null;

		FightRequest.currentRound = 0;
		FightRequest.preparatoryRounds = 0;
		FightRequest.consultScriptThatDidNothing = null;
	}

	private static final int getActionCost()
	{
		if ( FightRequest.action1.startsWith( "skill" ) )
		{
			String skillId = FightRequest.action1.substring( 5 );
			return SkillDatabase.getMPConsumptionById( StringUtilities.parseInt( skillId ) );
		}

		return 0;
	}

	public static void addItemActionsWithNoCost()
	{
		KoLCharacter.battleSkillNames.add( "item seal tooth" );
		KoLCharacter.battleSkillNames.add( "item turtle totem" );
		KoLCharacter.battleSkillNames.add( "item spices" );

		KoLCharacter.battleSkillNames.add( "item dictionary" );
		KoLCharacter.battleSkillNames.add( "item jam band flyers" );
		KoLCharacter.battleSkillNames.add( "item rock band flyers" );

		KoLCharacter.battleSkillNames.add( "item toy soldier" );
		KoLCharacter.battleSkillNames.add( "item toy mercenary" );

		KoLCharacter.battleSkillNames.add( "item Miniborg stomper" );
		KoLCharacter.battleSkillNames.add( "item Miniborg laser" );
		KoLCharacter.battleSkillNames.add( "item Miniborg Destroy-O-Bot" );

		KoLCharacter.battleSkillNames.add( "item naughty paper shuriken" );
		KoLCharacter.battleSkillNames.add( "item bottle of G&uuml;-Gone" );
	}

	private static final boolean isItemConsumed( final int itemId, final String responseText )
	{
		if ( ItemDatabase.getAttribute( itemId, ItemDatabase.ATTR_COMBAT_REUSABLE ) )
		{
			return false;
		}

		switch ( itemId )
		{
		case ItemPool.COMMUNICATIONS_WINDCHIMES:

			// Only record usage in battle if you got some sort of
			// response.
			//
			// You bang out a series of chimes, (success)
			//   or
			// A nearby hippy soldier sees you about to start
			// ringing your windchimes (failure)
			if ( responseText.indexOf( "bang out a series of chimes" ) != -1 ||
			     responseText.indexOf( "ringing your windchimes" ) != -1 )
			{
				IslandDecorator.ensureUpdatedBigIsland();
				Preferences.setInteger( "lastHippyCall", KoLAdventure.getAdventureCount() );
				// "Safe" interval between uses is 10 turns
				// http://alliancefromhell.com/forum/viewtopic.php?t=1398
				TurnCounter.stopCounting( "Communications Windchimes" );
				TurnCounter.startCounting( 10, "Communications Windchimes loc=*", "chimes.gif" );
			}

			// Then he takes your windchimes and wanders off.
			if ( responseText.indexOf( "he takes your windchimes" ) != -1 )
			{
				return true;
			}
			return false;

		case ItemPool.PADL_PHONE:

			// Only record usage in battle if you got some sort of
			// response.
			//
			// You punch a few buttons on the phone, (success)
			//   or
			// A nearby frat soldier sees you about to send a
			// message to HQ (failure)
			if ( responseText.indexOf( "punch a few buttons on the phone" ) != -1 ||
			     responseText.indexOf( "send a message to HQ" ) != -1 )
			{
				IslandDecorator.ensureUpdatedBigIsland();
				Preferences.setInteger( "lastFratboyCall", KoLAdventure.getAdventureCount() );
				// "Safe" interval between uses is 10 turns
				// http://alliancefromhell.com/forum/viewtopic.php?t=1398
				TurnCounter.stopCounting( "PADL Phone" );
				TurnCounter.startCounting( 10, "PADL Phone loc=*", "padl.gif" );
			}

			// Then he takes your phone and wanders off.
			if ( responseText.indexOf( "he takes your phone" ) != -1 )
			{
				return true;
			}
			return false;

		case ItemPool.HAROLDS_BELL:

			TurnCounter.startCounting( 20, "Harold's Bell loc=*", "bell.gif" );
			return true;

		case ItemPool.SPOOKY_PUTTY_SHEET:
			// You press the sheet of spooky putty against
			// him/her/it and make a perfect copy, which you shove
			// into your sack. He doesn't seem to appreciate it too
			// much...

			if ( responseText.indexOf( "make a perfect copy" ) != -1 )
			{
				Preferences.increment( "spookyPuttyCopiesMade", 1 );
				Preferences.setString( "spookyPuttyMonster", FightRequest.encounterLookup );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			return false;

		case ItemPool.CAMERA:
			// With a flash of light and an accompanying old-timey
			// -POOF- noise, you take snap a picture of him. Your
			// camera begins to shake, rattle and roll.

			if ( responseText.indexOf( "old-timey <i>-POOF-</i> noise" ) != -1 )
			{
				Preferences.setString( "cameraMonster", FightRequest.encounterLookup );
				Preferences.setString( "autoPutty", "" );
				return true;
			}
			return false;

		case ItemPool.ANTIDOTE: // Anti-Anti-Antidote
			// You quickly quaff the anti-anti-antidote. You feel better.

			return responseText.indexOf( "You quickly quaff" ) != -1;

		case ItemPool.MERKIN_PINKSLIP:

			// You hand him the pinkslip. He reads it, frowns, and
			// swims sulkily away.

			if ( responseText.indexOf( "swims sulkily away" ) != -1 )
			{
				return true;
			}

			return false;

		default:

			return true;
		}
	}

	private static final boolean shouldUseAntidote()
	{
		if ( !KoLConstants.inventory.contains( FightRequest.ANTIDOTE ) )
		{
			return false;
		}
		if ( KoLConstants.activeEffects.contains( FightRequest.BIRDFORM ) )
		{
			return false;	// can't use items!
		}
		int minLevel = Preferences.getInteger( "autoAntidote" );
		for ( int i = 0; i < KoLConstants.activeEffects.size(); ++i )
		{
			if ( AdventureSelectPanel.getPoisonLevel( ( (AdventureResult) KoLConstants.activeEffects.get( i ) ).getName() ) <= minLevel )
			{
				return true;
			}
		}
		return false;
	}

	private static final void payActionCost( final String responseText )
	{
		// If we don't know what we tried, punt now.
		if ( FightRequest.action1 == null || FightRequest.action1.equals( "" ) )
		{
			return;
		}

		// If we have Cunctatitis and decide to procrastinate, we did
		// nothing
		if ( KoLConstants.activeEffects.contains( FightRequest.CUNCTATITIS ) && responseText.indexOf( "You decide to" ) != -1 )
		{
			return;
		}

		switch ( KoLCharacter.getFamiliar().getId() )
		{
		case FamiliarPool.BLACK_CAT:
			// If we are adventuring with a Black Cat, she might
			// prevent skill and item use during combat.

			// <Name> jumps onto the keyboard and causes you to
			// accidentally hit the Attack button instead of using
			// that skill.

			if ( responseText.indexOf( "jumps onto the keyboard" ) != -1 )
			{
				FightRequest.action1 = "attack";
				return;
			}

			// Just as you're about to use that item, <name> bats
			// it out of your hand, and you have to spend the next
			// couple of minutes fishing it out from underneath a
			// couch. It's as adorable as it is annoying.

			if ( responseText.indexOf( "bats it out of your hand" ) != -1 )
			{
				return;
			}
			break;

		case FamiliarPool.OAF:
			// If we are adventuring with a O.A.F., it might
			// prevent skill and item use during combat.

			// Use of that skill has been calculated to be
			// sub-optimal. I recommend that you attack with your
			// weapon, instead.

			// Use of that item has been calculated to be
			// sub-optimal. I recommend that you attack with your
			// weapon, instead.

			if ( responseText.indexOf( "calculated to be sub-optimal" ) != -1 )
			{
				FightRequest.action1 = "attack";
				return;
			}

			break;
		}

		if ( FightRequest.action1.equals( "attack" ) ||
		     FightRequest.action1.equals( "runaway" ) ||
		     FightRequest.action1.equals( "steal" ) ||
		     FightRequest.action1.equals( "summon ghost" )   )
		{
			return;
		}

		if ( FightRequest.action1.equals( "jiggle" ) )
		{
			FightRequest.jiggledChefstaff = true;
			return;
		}

		if ( !FightRequest.action1.startsWith( "skill" ) )
		{
			if ( FightRequest.currentRound == 0 )
			{
				return;
			}

			FightRequest.payItemCost( StringUtilities.parseInt( FightRequest.action1 ), responseText );

			if ( FightRequest.action2 == null || FightRequest.action2.equals( "" ) )
			{
				return;
			}

			FightRequest.payItemCost( StringUtilities.parseInt( FightRequest.action2 ), responseText );

			return;
		}

		if ( responseText.indexOf( "You don't have that skill" ) != -1 )
		{
			return;
		}

		int skillId = StringUtilities.parseInt( FightRequest.action1.substring( 5 ) );
		int mpCost = SkillDatabase.getMPConsumptionById( skillId );

		if ( mpCost > 0 )
		{
			ResultProcessor.processResult( new AdventureResult( AdventureResult.MP, 0 - mpCost ) );
		}

		// As you're preparing to use that skill, The Bonerdagon
		// suddenly starts furiously beating its wings. You're knocked
		// over by the gust of wind it creates, and lose track of what
		// you're doing.

		if ( responseText.indexOf( "Bonerdagon suddenly starts furiously beating its wings" ) != -1 )
		{
			return;
		}

		switch ( skillId )
		{
		case 49:   // Gothy Handwave
			NemesisDecorator.useGothyHandwave( FightRequest.encounterLookup, responseText );
			break;

		case 2005: // Shieldbutt
		case 2105: // Head + Shield Combo
		case 2106: // Knee + Shield Combo
		case 2107: // Head + Knee + Shield Combo
			FightRequest.levelModifier -= 5;
			break;

		case 5003: // Disco Eye-Poke
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 3;
			break;

		case 5005: // Disco Dance of Doom
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 5;
			break;

		case 5008: // Disco Dance II: Electric Boogaloo
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 7;
			break;

		case 5012: // Disco Face Stab
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 10;
			break;

		case 5019: // Tango of Terror
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 8;
			break;

		case 5021: // Suckerpunch
			FightRequest.levelModifier -= FightRequest.stealthMistletoe * 1;
			break;

		case 5023: // Stealth Mistletoe
			FightRequest.stealthMistletoe = 2;
			break;

		case 7038: // Vicious Talon Slash
		case 7039: // All-You-Can-Beat Wing Buffet
			Preferences.increment( "birdformRoc", 1 );
			break;

		case 7040: // Tunnel Upwards
			Preferences.increment( "moleTunnelLevel", 1 );
			break;

		case 7041: // Tunnel Downwards
			Preferences.increment( "moleTunnelLevel", -1 );
			break;

		case 7042: // Rise From Your Ashes
			Preferences.increment( "birdformHot", 1 );
			break;

		case 7043: // Antarctic Flap
			Preferences.increment( "birdformCold", 1 );
			break;

		case 7044: // The Statue Treatment
			Preferences.increment( "birdformStench", 1 );
			break;

		case 7045: // Feast on Carrion
			Preferences.increment( "birdformSpooky", 1 );
			break;

		case 7046: // Give Your Opponent "The Bird"
			Preferences.increment( "birdformSleaze", 1 );
			break;

		case 7050: // Ask the hobo to tell you a joke
			Modifiers.overrideModifier( "fightMods", "Meat Drop: +100" );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			break;

		case 7051: // Ask the hobo to dance for you
			Modifiers.overrideModifier( "fightMods", "Item Drop: +100" );
			KoLCharacter.recalculateAdjustments();
			KoLCharacter.updateStatus();
			break;

		case 7082:	// Point at your opponent
			String type;
			if ( responseText.indexOf( "firing a searing ray" ) != -1 )
			{
				type = "<font color=red>Major Red Recharge</font>";
			}
			else if ( responseText.indexOf( "blue light" ) != -1 )
			{
				type = "<font color=blue>Major Blue Recharge</font>";
			}
			else if ( responseText.indexOf( "yellow energy" ) != -1 )
			{
				type = "<font color=olive>Major Yellow Recharge</font>";
			}
			else break;
			int cooldown = KoLCharacter.hasEquipped( ItemPool.get(
				ItemPool.QUADROCULARS, 1 ) ) ? 101 : 150;
			TurnCounter.stopCounting( type );
			TurnCounter.startCounting( cooldown, type + " loc=*", "heboulder.gif" );
		}
	}

	public static final void payItemCost( final int itemId, final String responseText )
	{
		if ( itemId <= 0 )
		{
			return;
		}

		switch ( itemId )
		{
		case ItemPool.TOY_SOLDIER:
			// A toy soldier consumes tequila.

			if ( KoLConstants.inventory.contains( FightRequest.TEQUILA ) )
			{
				ResultProcessor.processResult( FightRequest.TEQUILA );
			}
			break;

		case ItemPool.TOY_MERCENARY:
			// A toy mercenary consumes 5-10 meat

			// A sidepane refresh at the end of the battle will
			// re-synch everything.
			break;

		case ItemPool.SHRINKING_POWDER:
			if ( responseText.indexOf( "gets smaller and angrier" ) != -1 )
			{
				FightRequest.healthModifier += FightRequest.getMonsterHealth() / 2;
			}
			break;
		}

		if ( FightRequest.isItemConsumed( itemId, responseText ) )
		{
			ResultProcessor.processResult( new AdventureResult( itemId, -1 ) );
			return;
		}
	}

	public int getAdventuresUsed()
	{
		return 0;
	}

	public static final String getNextTrackedRound()
	{
		while ( FightRequest.isTrackingFights && !FightRequest.foundNextRound && !KoLmafia.refusesContinue() )
		{
			PAUSER.pause( 200 );
		}

		if ( !FightRequest.foundNextRound || KoLmafia.refusesContinue() )
		{
			FightRequest.isTrackingFights = false;
		}
		else if ( FightRequest.isTrackingFights )
		{
			FightRequest.isTrackingFights = FightRequest.currentRound != 0;
		}

		FightRequest.foundNextRound = false;
		return RequestEditorKit.getFeatureRichHTML(
			FightRequest.isTrackingFights ? "fight.php?action=script" : "fight.php",
			FightRequest.lastResponseText,
			true );
	}

	public static final int getCurrentRound()
	{
		return FightRequest.currentRound;
	}

	public static final boolean alreadyJiggled()
	{
		return FightRequest.jiggledChefstaff;
	}

	public static final void beginTrackingFights()
	{
		FightRequest.isTrackingFights = true;
		FightRequest.foundNextRound = false;
	}

	public static final void stopTrackingFights()
	{
		FightRequest.isTrackingFights = false;
		FightRequest.foundNextRound = false;
	}

	public static final boolean isTrackingFights()
	{
		return FightRequest.isTrackingFights;
	}

	public static final boolean haveFought()
	{
		boolean rv = FightRequest.haveFought;
		FightRequest.haveFought = false;
		return rv;
	}

	public static final String getLastMonsterName()
	{
		return FightRequest.encounterLookup;
	}

	public static final Monster getLastMonster()
	{
		return FightRequest.monsterData;
	}

	public static final int freeRunawayChance()
	{
		// Bandersnatch + Ode = weight/5 free runaways
		if ( KoLCharacter.getFamiliar().getId() == FamiliarPool.BANDER &&
			KoLConstants.activeEffects.contains( ItemDatabase.ODE ) )
		{
			if ( !FightRequest.castCleesh &&
				KoLCharacter.getFamiliar().getModifiedWeight() / 5 >
				Preferences.getInteger( "_banderRunaways" ) )
			{
				return 100;
			}
		}
		else if ( KoLCharacter.hasEquipped( ItemPool.get( ItemPool.NAVEL_RING, 1 ) ) )
		{
			return Math.max( 20, 120 - 10 *
				Preferences.getInteger( "_navelRunaways" ) );
		}
		return 0;
	}

	public static final boolean registerRequest( final boolean isExternal, final String urlString )
	{
		if ( !urlString.startsWith( "fight.php" ) )
		{
			return false;
		}

		FightRequest.action1 = null;
		FightRequest.action2 = null;

		if ( urlString.equals( "fight.php" ) || urlString.indexOf( "ireallymeanit=" ) != -1 )
		{
			return true;
		}

		boolean shouldLogAction = Preferences.getBoolean( "logBattleAction" );
		StringBuffer action = new StringBuffer();

		// Begin logging all the different combat actions and storing
		// relevant data for post-processing.

		if ( shouldLogAction )
		{
			action.append( "Round " );
			action.append( FightRequest.currentRound );
			action.append( ": " );
			action.append( KoLCharacter.getUserName() );
			action.append( " " );
		}

		if ( urlString.indexOf( "runaway" ) != -1 )
		{
			FightRequest.action1 = "runaway";
			if ( shouldLogAction )
			{
				action.append( "casts RETURN!" );
			}
		}
		else if ( urlString.indexOf( "steal" ) != -1 )
		{
			FightRequest.action1 = "steal";
			if ( shouldLogAction )
			{
				action.append( "tries to steal an item!" );
			}
		}
		else if ( urlString.indexOf( "attack" ) != -1 )
		{
			FightRequest.action1 = "attack";
			if ( shouldLogAction )
			{
				action.append( "attacks!" );
			}
		}
		else if ( urlString.indexOf( "summon" ) != -1 )
		{
			FightRequest.action1 = "summon ghost";
			if ( shouldLogAction )
			{
				action.append( "summons " );
				action.append( Preferences.getString( "pastamancerGhostName" ) );
				action.append( " the " );
				action.append( Preferences.getString( "pastamancerGhostType" ) );
				action.append( "!" );
			}
		}
		else if ( urlString.indexOf( "chefstaff" ) != -1 )
		{
			FightRequest.action1 = "jiggle";
			if ( shouldLogAction )
			{
				action.append( "jiggles the " );
				action.append( EquipmentManager.getEquipment( EquipmentManager.WEAPON ).getName() );
			}
		}
		else
		{
			Matcher skillMatcher = FightRequest.SKILL_PATTERN.matcher( urlString );
			if ( skillMatcher.find() )
			{
				String skillId = skillMatcher.group( 1 );
				if ( FightRequest.isInvalidAttack( skillId ) )
				{
					return true;
				}

				String skill = SkillDatabase.getSkillName( StringUtilities.parseInt( skillId ) );
				if ( skill == null )
				{
					if ( shouldLogAction )
					{
						action.append( "casts CHANCE!" );
					}
				}
				else
				{
					if ( skillId.equals( "19" ) )
					{
						if ( !KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
						{
							Preferences.setString( "olfactedMonster", FightRequest.encounterLookup );
							Preferences.setString( "autoOlfact", "" );
							FightRequest.canOlfact = false;
						}
					}
					else if ( skillId.equals( "3004" ) )
					{
						FightRequest.castNoodles = true;
					}

					FightRequest.action1 = CustomCombatManager.getShortCombatOptionName( "skill " + skill );
					if ( shouldLogAction )
					{
						action.append( "casts " + skill.toUpperCase() + "!" );
					}
				}
			}
			else
			{
				Matcher itemMatcher = FightRequest.ITEM1_PATTERN.matcher( urlString );
				if ( itemMatcher.find() )
				{
					int itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
					String item = ItemDatabase.getItemName( itemId );
					if ( item == null )
					{
						if ( shouldLogAction )
						{
							action.append( "plays Garin's Harp" );
						}
					}
					else
					{
						if ( item.equalsIgnoreCase( "odor extractor" ) &&
							!KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
						{
							Preferences.setString( "olfactedMonster",
								FightRequest.encounterLookup );
							Preferences.setString( "autoOlfact", "" );
							FightRequest.canOlfact = false;
						}
						FightRequest.action1 = String.valueOf( itemId );
						if ( shouldLogAction )
						{
							action.append( "uses the " + item );
						}
					}

					itemMatcher = FightRequest.ITEM2_PATTERN.matcher( urlString );
					if ( itemMatcher.find() )
					{
						itemId = StringUtilities.parseInt( itemMatcher.group( 1 ) );
						item = ItemDatabase.getItemName( itemId );
						if ( item != null )
						{
							if ( item.equalsIgnoreCase( "odor extractor" ) &&
								!KoLConstants.activeEffects.contains( FightRequest.ONTHETRAIL ) )
							{
								Preferences.setString( "olfactedMonster",
									FightRequest.encounterLookup );
								Preferences.setString( "autoOlfact", "" );
							}
							FightRequest.action2 = String.valueOf( itemId );
							if ( shouldLogAction )
							{
								action.append( " and uses the " + item );
							}
						}
					}

					if ( shouldLogAction )
					{
						action.append( "!" );
					}
				}
			}
		}

		if ( shouldLogAction )
		{
			String message = action.toString();
			RequestLogger.printLine( message );
			RequestLogger.updateSessionLog( message );
		}

		return true;
	}

	// Log cleaned HTML

	private static final void logHTML( final TagNode node )
	{
		StringBuffer buffer = new StringBuffer();
		FightRequest.logHTML( node, buffer, 0 );
	}

	private static final void logHTML( final TagNode node, final StringBuffer buffer, int level )
	{
		String name = node.getName();

		// Skip scripts, forms, buttons, and html links
		if ( name.equals( "script" ) ||
		     name.equals( "form" ) ||
		     name.equals( "input" ) ||
		     name.equals( "a" ) ||
		     name.equals( "div" ) )
		{
			return;
		}

		FightRequest.indent( buffer, level );
		FightRequest.printTag( buffer, node );
		RequestLogger.updateDebugLog( buffer.toString() );

		Iterator it = node.getChildren().iterator();
		while ( it.hasNext() )
		{
			Object child = it.next();

			if ( child instanceof CommentToken )
			{
				CommentToken object = (CommentToken) child;
				String content = object.getContent();
				FightRequest.indent( buffer, level + 1 );
				buffer.append( "<!--" );
				buffer.append( content );
				buffer.append( "-->" );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof ContentToken )
			{
				ContentToken object = (ContentToken) child;
				String content = object.getContent().trim();
				if ( content.equals( "" ) )
				{
					continue;
				}

				FightRequest.indent( buffer, level + 1 );
				buffer.append( content );
				RequestLogger.updateDebugLog( buffer.toString() );
				continue;
			}

			if ( child instanceof TagNode )
			{
				TagNode object = (TagNode) child;
				FightRequest.logHTML( object, buffer, level + 1 );
				continue;
			}
		}
	}

	private static final void indent( final StringBuffer buffer, int level )
	{
		buffer.setLength( 0 );
		for ( int i = 0; i < level; ++i )
		{
			buffer.append( " " );
			buffer.append( " " );
		}
	}

	private static final void printTag( final StringBuffer buffer, TagNode node )
	{
		String name = node.getName();
		Map attributes = node.getAttributes();

		buffer.append( "<" );
		buffer.append( name );

		if ( !attributes.isEmpty() )
		{
			Iterator it = attributes.keySet().iterator();
			while ( it.hasNext() )
			{
				String key = (String) it.next();
				buffer.append( " " );
				buffer.append( key );
				buffer.append( "=\"" );
				buffer.append( (String) attributes.get( key ) );
				buffer.append( "\"" );
			}
		}
		buffer.append( ">" );
	}
}
