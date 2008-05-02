/**
 * Copyright (c) 2005-2008, KoLmafia development team
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

package net.sourceforge.kolmafia.session;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.swingui.CouncilFrame;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public abstract class ChoiceManager
{
	private static final AdventureResult PAPAYA = new AdventureResult( 498, 1 );
	private static final Pattern CHOICE_PATTERN = Pattern.compile( "whichchoice value=(\\d+)" );
	public static final GenericRequest CHOICE_HANDLER = new GenericRequest( "choice.php" );

	private static final AdventureResult MAIDEN_EFFECT = new AdventureResult( "Dreams and Lights", 1, true );
	private static final AdventureResult BALLROOM_KEY = new AdventureResult( 1766, 1 );

	private static final AdventureResult[] MISTRESS_ITEMS = new AdventureResult[]
	{
		new AdventureResult( 1941, 1 ),
		new AdventureResult( 1942, 1 ),
		new AdventureResult( 1943, 1 ),
		new AdventureResult( 1944, 1 ),
		new AdventureResult( 1945, 1 ),
		new AdventureResult( 1946, 1 ),
		new AdventureResult( 2092, 1 )
	};

	public static class ChoiceAdventure
		implements Comparable
	{
		private final String zone;
		private final String setting;
		private final String name;

		private final String[] options;
		private final String[] items;
		private String[][] spoilers;

		public ChoiceAdventure( final String setting, final String name, final String[] options )
		{
			this( "Unsorted", setting, name, options, null );
		}

		public ChoiceAdventure( final String setting, final String name, final String[] options, final String[] items )
		{
			this( "Unsorted", setting, name, options, items );
		}

		public ChoiceAdventure( final String zone, final String setting, final String name, final String[] options )
		{
			this( zone, setting, name, options, null );
		}

		public ChoiceAdventure( final String zone, final String setting, final String name, final String[] options,
			final String[] items )
		{
			this.zone = (String) AdventureDatabase.PARENT_ZONES.get( zone );
			this.setting = setting;
			this.name = name;
			this.options = options;
			this.items = items;

			String[] settingArray = new String[] { setting };

			String[] nameArray = new String[] { name };

			if ( items != null )
			{
				this.spoilers = new String[][] { settingArray, nameArray, options, items };
			}
			else
			{
				this.spoilers = new String[][] { settingArray, nameArray, options };
			}
		}

		public String getZone()
		{
			return this.zone;
		}

		public String getSetting()
		{
			return this.setting;
		}

		public String getName()
		{
			return this.name;
		}

		public String[] getItems()
		{
			return this.items;
		}

		public String[] getOptions()
		{
			if ( this.options == null )
			{
				return ChoiceManager.dynamicChoiceOptions( this.setting );
			}
			return this.options;
		}

		public String[][] getSpoilers()
		{
			return this.spoilers;
		}

		public int compareTo( final Object o )
		{
			if ( ChoiceManager.choicesOrderedByName )
			{
				int result = this.name.compareToIgnoreCase( ( (ChoiceAdventure) o ).name );

				if ( result != 0 )
				{
					return result;
				}
			}

			int a = StringUtilities.parseInt( this.setting.substring( 15 ) );
			int b = StringUtilities.parseInt( ( (ChoiceAdventure) o ).setting.substring( 15 ) );

			return a - b;
		}
	}

	private static boolean choicesOrderedByName = true;

	public static final void setChoiceOrdering( final boolean choicesOrderedByName )
	{
		ChoiceManager.choicesOrderedByName = choicesOrderedByName;
	}

	// Lucky sewer options
	public static final ChoiceAdventure LUCKY_SEWER =
		new ChoiceAdventure( "Town", "luckySewerAdventure",
				     "Sewer Gnomes",
				     new String[] {
					     "seal-clubbing club",
					     "seal-skull helmet",
					     "helmet turtle",
					     "turtle totem",
					     "pasta spoon",
					     "ravioli hat",
					     "saucepan",
					     "disco mask",
					     "disco ball",
					     "stolen accordion",
					     "mariachi pants"
				     } );

	public static final ChoiceAdventure[] CHOICE_ADVS =
	{
		// Choice 1 is unknown
		// Choice 2 is Denim Axes Examined
		// Choice 3 is The Oracle Will See You Now

		// Finger-Lickin'... Death.
		new ChoiceAdventure(
			"Beach", "choiceAdventure4", "South of the Border",
			new String[] { "small meat boost", "try for poultrygeist", "skip adventure" },
			new String[] { null, "1164", null } ),

		// Heart of Very, Very Dark Darkness
		new ChoiceAdventure(
			"MusSign", "choiceAdventure5", "Gravy Barrow",
			new String[] { "fight the fairy queen", "skip adventure" } ),

		// Choice 6 is Darker Than Dark
		// Choice 7 is How Depressing
		// Choice 8 is On the Verge of a Dirge
		// Choice 9 is the Giant Castle Chore Wheel: muscle position
		// Choice 10 is the Giant Castle Chore Wheel: mysticality position
		// Choice 11 is the Giant Castle Chore Wheel: map quest position
		// Choice 12 is the Giant Castle Chore Wheel: moxie position

		// Choice 13 is unknown

		// A Bard Day's Night
		new ChoiceAdventure(
			"Knob", "choiceAdventure14", "Knob Goblin Harem",
			new String[] { "Knob goblin harem veil", "Knob goblin harem pants", "small meat boost", "complete the outfit" },
			new String[] { "306", "305", null } ),

		// Yeti Nother Hippy
		new ChoiceAdventure(
			"Mountain", "choiceAdventure15", "eXtreme Slope",
			new String[] { "eXtreme mittens", "eXtreme scarf", "small meat boost", "complete the outfit" },
			new String[] { "399", "355", null } ),

		// Saint Beernard
		new ChoiceAdventure(
			"Mountain", "choiceAdventure16", "eXtreme Slope",
			new String[] { "snowboarder pants", "eXtreme scarf", "small meat boost", "complete the outfit" },
			new String[] { "356", "355", null } ),

		// Generic Teen Comedy
		new ChoiceAdventure(
			"Mountain", "choiceAdventure17", "eXtreme Slope",
			new String[] { "eXtreme mittens", "snowboarder pants", "small meat boost", "complete the outfit" },
			new String[] { "399", "356", null } ),

		// A Flat Miner
		new ChoiceAdventure(
			"Mountain", "choiceAdventure18", "Itznotyerzitz Mine",
			new String[] { "miner's pants", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
			new String[] { "361", "362", null } ),

		// 100% Legal
		new ChoiceAdventure(
			"Mountain", "choiceAdventure19", "Itznotyerzitz Mine",
			new String[] { "miner's helmet", "miner's pants", "small meat boost", "complete the outfit" },
			new String[] { "360", "361", null } ),

		// See You Next Fall
		new ChoiceAdventure(
			"Mountain", "choiceAdventure20", "Itznotyerzitz Mine",
			new String[] { "miner's helmet", "7-Foot Dwarven mattock", "small meat boost", "complete the outfit" },
			new String[] { "360", "362", null } ),

		// Under the Knife
		new ChoiceAdventure(
			"Town", "choiceAdventure21", "Sleazy Back Alley",
			new String[] { "switch genders", "skip adventure" } ),

		// The Arrrbitrator
		new ChoiceAdventure(
			"Island", "choiceAdventure22", "Pirate's Cove",
			new String[] { "eyepatch", "swashbuckling pants", "small meat boost", "complete the outfit" },
			new String[] { "224", "402", null } ),

		// Barrie Me at Sea
		new ChoiceAdventure(
			"Island",
			"choiceAdventure23",
			"Pirate's Cove",
			new String[] { "stuffed shoulder parrot", "swashbuckling pants", "small meat boost", "complete the outfit" },
			new String[] { "403", "402", null } ),

		// Amatearrr Night
		new ChoiceAdventure(
			"Island", "choiceAdventure24", "Pirate's Cove",
			new String[] { "stuffed shoulder parrot", "small meat boost", "eyepatch", "complete the outfit" },
			new String[] { "403", null, "224" } ),

		// Choice 25 is Ouch! You bump into a door!
		// Choice 26 is A Three-Tined Fork
		// Choice 27 is Footprints
		// Choice 28 is A Pair of Craters
		// Choice 29 is The Road Less Visible

		// Choices 30 - 39 are unknown

		// The Effervescent Fray
		new ChoiceAdventure(
			"Plains", "choiceAdventure40", "Cola Wars",
			new String[] { "Cloaca-Cola fatigues", "Dyspepsi-Cola shield", "mysticality substats" },
			new String[] { "1328", "1329", null } ),

		// Smells Like Team Spirit
		new ChoiceAdventure(
			"Plains", "choiceAdventure41", "Cola Wars",
			new String[] { "Dyspepsi-Cola fatigues", "Cloaca-Cola helmet", "muscle substats" },
			new String[] { "1330", "1331", null } ),

		// What is it Good For?
		new ChoiceAdventure(
			"Plains", "choiceAdventure42", "Cola Wars",
			new String[] { "Dyspepsi-Cola helmet", "Cloaca-Cola shield", "moxie substats" },
			new String[] { "1326", "1327", null } ),

		// Choices 43 - 44 are unknown
		// Choice 45 is Maps and Legends

		// An Interesting Choice
		new ChoiceAdventure(
			"Woods", "choiceAdventure46", "Spooky Forest",
			new String[] { "moxie substats", "muscle substats", "vampire heart" },
			new String[] { null, null, "1518" } ),

		// Have a Heart
		new ChoiceAdventure(
			"Woods", "choiceAdventure47", "Spooky Forest", new String[] { "bottle of used blood", "skip adventure" },
			new String[] { "1523", "1518" } ),

		// Choices 48 - 70 are violet fog adventures
		// Choice 71 is A Journey to the Center of Your Mind

		// Lording Over The Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure72", "Frat House", new String[] { "around the world", "skip adventure" },
			new String[] { "1634", "1633" } ),

		// Don't Fence Me In
		new ChoiceAdventure(
			"Woods", "choiceAdventure73", "Whitey's Grove",
			new String[] { "muscle substats", "white picket fence", "piece of wedding cake" },
			new String[] { null, "270", "262" } ),

		// The Only Thing About Him is the Way That He Walks
		new ChoiceAdventure(
			"Woods", "choiceAdventure74", "Whitey's Grove",
			new String[] { "moxie substats", "boxed wine", "mullet wig" },
			new String[] { null, "1005", "267" } ),

		// Rapido!
		new ChoiceAdventure(
			"Woods", "choiceAdventure75", "Whitey's Grove",
			new String[] { "mysticality substats", "white lightning", "white collar" },
			new String[] { null, "266", "1655" } ),

		// Junction in the Trunction
		new ChoiceAdventure(
			"Knob", "choiceAdventure76", "Knob Shaft",
			new String[] { "cardboard ore", "styrofoam ore", "bubblewrap ore" },
			new String[] { "1675", "1676", "1677" } ),

		// Choice 77 is Minnesota Incorporeals
		// Choice 78 is Broken
		// Choice 79 is A Hustle Here, a Hustle There
		// Choice 80 is Take a Look, it's in a Book!
		// Choice 81 is Take a Look, it's in a Book!

		// One NightStand (simple white)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure82", "Haunted Bedroom",
			new String[] { "old leather wallet", "muscle substats", "enter combat" },
			new String[] { "1917", null, null } ),

		// One NightStand (mahogany)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure83", "Haunted Bedroom",
			new String[] { "old coin purse", "enter combat", "quest item" },
			new String[] { "1918", null, null } ),

		// One NightStand (ornate)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure84", "Haunted Bedroom",
			new String[] { "small meat boost", "mysticality substats", "Lord Spookyraven's spectacles" },
			new String[] { null, null, "1916" } ),

		// One NightStand (simple wooden)
		new ChoiceAdventure(
			"Manor2", "choiceAdventure85", "Haunted Bedroom",
			new String[] { "moxie (ballroom key step 1)", "empty drawer (ballroom key step 2)", "enter combat" } ),

		// Choice 86 is History is Fun!
		// Choice 87 is History is Fun!
		// Choice 88 is Naughty, Naughty
		// Choice 89 is Out in the Garden

		// Curtains
		new ChoiceAdventure(
			"Manor2", "choiceAdventure90", "Haunted Ballroom",
			new String[] { "enter combat", "moxie substats", "skip adventure" } ),

		// Choice 91 is Louvre It or Leave It
		// Choices 92 - 104 are Escher print adventures

		// Having a Medicine Ball
		new ChoiceAdventure(
			"Manor2", "choiceAdventure105", "Haunted Bathroom",
			new String[] { "moxie substats", "other options", "guy made of bees" } ),

		// Strung-Up Quartet
		new ChoiceAdventure(
			"Manor2", "choiceAdventure106", "Haunted Ballroom",
			new String[] { "increase monster level", "decrease combat frequency", "increase item drops", "skip adventure" } ),

		// Bad Medicine is What You Need
		new ChoiceAdventure(
			"Manor2", "choiceAdventure107", "Haunted Bathroom",
			new String[] { "antique bottle of cough syrup", "tube of hair oil", "bottle of ultravitamins", "skip adventure" },
			new String[] { "2086", "2087", "2085", null } ),

		// Aww, Craps
		new ChoiceAdventure(
			"Town", "choiceAdventure108", "Sleazy Back Alley",
			new String[] { "moxie substats", "meat and moxie", "random effect", "skip adventure" } ),

		// Dumpster Diving
		new ChoiceAdventure(
			"Town", "choiceAdventure109", "Sleazy Back Alley",
			new String[] { "enter combat", "meat and moxie", "Mad Train wine" },
			new String[] { null, null, "564" } ),

		// The Entertainer
		new ChoiceAdventure(
			"Town", "choiceAdventure110", "Sleazy Back Alley",
			new String[] { "moxie substats", "moxie and muscle", "small meat boost", "skip adventure" } ),

		// Malice in Chains
		new ChoiceAdventure(
			"Knob", "choiceAdventure111", "Knob Outskirts",
			new String[] { "muscle substats", "muscle substats", "enter combat" } ),

		// Please, Hammer
		new ChoiceAdventure(
			"Town", "choiceAdventure112", "Sleazy Back Alley",
			new String[] { "accept hammer quest", "reject quest", "muscle substats" } ),

		// Knob Goblin BBQ
		new ChoiceAdventure(
			"Knob", "choiceAdventure113", "Knob Outskirts",
			new String[] { "complete cake quest", "enter combat", "get a random item" } ),

		// The Baker's Dilemma
		new ChoiceAdventure(
			"Manor1", "choiceAdventure114", "Haunted Pantry",
			new String[] { "accept cake quest", "reject quest", "moxie and meat" } ),

		// Oh No, Hobo
		new ChoiceAdventure(
			"Manor1", "choiceAdventure115", "Haunted Pantry",
			new String[] { "enter combat", "Good Karma", "mysticality, moxie, and meat" } ),

		// The Singing Tree
		new ChoiceAdventure(
			"Manor1", "choiceAdventure116", "Haunted Pantry",
			new String[] { "mysticality substats", "moxie substats", "random effect", "skip adventure" } ),

		// Tresspasser
		new ChoiceAdventure(
			"Manor1", "choiceAdventure117", "Haunted Pantry",
			new String[] { "enter combat", "mysticality substats", "get a random item" } ),

		// When Rocks Attack
		new ChoiceAdventure(
			"Knob", "choiceAdventure118", "Knob Outskirts",
			new String[] { "accept unguent quest", "skip adventure" } ),

		// Choice 118 is When Rocks Attack
		// Choice 119 is unknown

		// Ennui is Wasted on the Young
		new ChoiceAdventure(
			"Knob", "choiceAdventure120", "Knob Outskirts",
			new String[] { "muscle and Pumped Up", "ice cold Sir Schlitz", "moxie and lemon", "skip adventure" },
			new String[] { null, "41", "332", null } ),

		// Choice 121 is Next Sunday, A.D.
		// Choice 122 is unknown
		// Choice 123 is At Least It's Not Full Of Trash
		// Choice 124 is unknown
		// Choice 125 is No Visible Means of Support

		// Sun at Noon, Tan Us
		new ChoiceAdventure(
			"Plains", "choiceAdventure126", "Palindome",
			new String[] { "moxie", "chance of more moxie", "sunburned" } ),

		// Choice 127 is No sir, away!	A papaya war is on!
		// Choice 128 is unknown
		// Choice 129 is Do Geese See God?
		// Choice 130 is Rod Nevada, Vendor
		// Choice 131 is Dr. Awkward

		// Let's Make a Deal!
		new ChoiceAdventure(
			"Beach", "choiceAdventure132", "Desert (Pre-Oasis)",
			new String[] { "broken carburetor", "Unlock Oasis" },
			new String[] { "2316", null } ),

		// Choice 133 is unknown

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure134", "The Middle Chamber",
			new String[] { "Turn the wheel", "skip adventure" } ),

		// Wheel In the Pyramid, Keep on Turning
		new ChoiceAdventure(
			"Pyramid", "choiceAdventure135", "The Middle Chamber",
			new String[] { "Turn the wheel", "skip adventure" } ),

		// Peace Wants Love
		new ChoiceAdventure(
			"Island", "choiceAdventure136", "Hippy Camp",
			new String[] { "filthy corduroys", "filthy knitted dread sack", "small meat boost", "complete the outfit" },
			new String[] { "213", "214", null } ),

		// An Inconvenient Truth
		new ChoiceAdventure(
			"Island", "choiceAdventure137", "Hippy Camp",
			new String[] { "filthy knitted dread sack", "filthy corduroys", "small meat boost", "complete the outfit" },
			new String[] { "214", "213", null } ),

		// Purple Hazers
		new ChoiceAdventure(
			"Island", "choiceAdventure138", "Frat House",
			new String[] { "orcish cargo shorts", "Orcish baseball cap", "homoerotic frat-paddle", "complete the outfit" },
			new String[] { "240", "239", "241" } ),

		// Bait and Switch
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure139", "War Hippies",
			new String[] { "muscle substats", "ferret bait", "enter combat" },
			new String[] { null, "2041", null } ),

		// The Thin Tie-Dyed Line
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure140", "War Hippies",
			new String[] { "water pipe bombs", "moxie substats", "enter combat" },
			new String[] { "2348", null, null } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure141", "War Hippies",
			new String[] { "mysticality substats", "get some hippy food", "waste a turn" } ),

		// Blockin' Out the Scenery
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure142", "War Hippies",
			new String[] { "mysticality substats", "get some hippy food", "start the war" } ),

		// Catching Some Zetas
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure143", "War Fraternity",
			new String[] { "muscle substats", "sake bombs", "enter combat" },
			new String[] { null, "2067", null } ),

		// One Less Room Than In That Movie
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure144", "War Fraternity",
			new String[] { "moxie substats", "beer bombs", "enter combat" },
			new String[] { null, "2350", null } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure145", "War Fraternity",
			new String[] { "muscle substats", "get some frat food", "waste a turn" },
			new String[] { null, null, null } ),

		// Fratacombs
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure146", "War Fraternity",
			new String[] { "muscle substats", "get some frat food", "start the war" } ),

		// Cornered!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure147", "Isle War Barn",
			new String[] { "Open The Granary (meat)", "Open The Bog (stench)", "Open The Pond (cold)" } ),

		// Cornered Again!
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure148", "Isle War Barn",
			new String[] { "Open The Back 40 (hot)", "Open The Family Plot (spooky)" } ),

		// ow Many Corners Does this Stupid Barn Have!?
		new ChoiceAdventure(
			"IsleWar", "choiceAdventure149", "Isle War Barn",
			new String[] { "Open The Shady Thicket (booze)", "Open The Other Back 40 (sleaze)" } ),

		// Choice 150 is Another Adventure About BorderTown

		// Adventurer, $1.99
		new ChoiceAdventure(
			"Plains", "choiceAdventure151", "Fun House",
			new String[] { "fight the clownlord", "skip adventure" } ),

		// Choice 152 is Lurking at the Threshold

		// Turn Your Head and Coffin
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure153", "Defiled Alcove",
			new String[] { "muscle substats", "small meat boost", "half-rotten brain", "skip adventure" },
			new String[] { null, null, "2562", null } ),

		// Doublewide
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure154", "Defiled Alcove",
			new String[] { "fight conjoined zmombie", "skip adventure" } ),

		// Skull, Skull, Skull
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure155", "Defiled Nook",
			new String[] { "moxie substats", "small meat boost", "rusty bonesaw", "skip adventure" },
			new String[] { null, null, "2563", null } ),

		// Pileup
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure156", "Defiled Nook",
			new String[] { "fight giant skeelton", "skip adventure" } ),

		// Urning Your Keep
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure157", "Defiled Niche",
			new String[] { "mysticality substats", "plus-sized phylactery", "small meat gain", "skip adventure" },
			new String[] { null, "2564", null, null } ),

		// Lich in the Niche
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure158", "Defiled Niche",
			new String[] { "fight gargantulihc", "skip adventure" } ),

		// Go Slow Past the Drawers
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure159", "Defiled Cranny",
			new String[] { "small meat boost", "stats & HP & MP", "can of Ghuol-B-Gone&trade;", "skip adventure" },
			new String[] { null, null, "2565", null } ),

		// Lunchtime
		new ChoiceAdventure(
			"Cyrpt", "choiceAdventure160", "Defiled Cranny",
			new String[] { "fight huge ghuol", "skip adventure" } ),

		// Choice 161 is Bureaucracy of the Damned
		// Choice 162 is Between a Rock and Some Other Rocks

		// Melvil Dewey Would Be Ashamed
		new ChoiceAdventure(
			"Manor1", "choiceAdventure163", "Haunted Library",
			new String[] { "Necrotelicomnicon", "Cookbook of the Damned", "Sinful Desires", "skip adventure" },
			new String[] { "2494", "2495", "2496", null } ),

		// The Wormwood choices always come in order

		// 1: 164, 167, 170
		// 2: 165, 168, 171
		// 3: 166, 169, 172

		// Some first-round choices give you an effect for five turns:

		// 164/2 -> Spirit of Alph
		// 167/3 -> Bats in the Belfry
		// 170/1 -> Rat-Faced

		// First-round effects modify some second round options and
		// give you a second effect for five rounds. If you do not have
		// the appropriate first-round effect, these second-round
		// options do not consume an adventure.

		// 165/1 + Rat-Faced -> Night Vision
		// 165/2 + Bats in the Belfry -> Good with the Ladies
		// 168/2 + Spirit of Alph -> Feelin' Philosophical
		// 168/2 + Rat-Faced -> Unusual Fashion Sense
		// 171/1 + Bats in the Belfry -> No Vertigo
		// 171/3 + Spirit of Alph -> Dancing Prowess

		// Second-round effects modify some third round options and
		// give you an item. If you do not have the appropriate
		// second-round effect, most of these third-round options do
		// not consume an adventure.

		// 166/1 + No Vertigo -> S.T.L.T.
		// 166/3 + Unusual Fashion Sense -> albatross necklace
		// 169/1 + Night Vision -> flask of Amontillado
		// 169/3 + Dancing Prowess -> fancy ball mask
		// 172/1 + Good with the Ladies -> Can-Can skirt
		// 172/1 -> combat
		// 172/2 + Feelin' Philosophical -> not-a-pipe

		// Down by the Riverside
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure164", "Pleasure Dome",
			new String[] { "muscle substats", "MP & Spirit of Alph", "enter combat" } ),

		// Beyond Any Measure
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure165", "Pleasure Dome",
			new String[] { "Rat-Faced -> Night Vision", "Bats in the Belfry -> Good with the Ladies", "mysticality substats", "skip adventure" } ),

		// Death is a Boat
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure166", "Pleasure Dome",
			new String[] { "No Vertigo -> S.T.L.T.", "moxie substats", "Unusual Fashion Sense -> albatross necklace" },
			new String[] { "2652", null, "2659" } ),

		// It's a Fixer-Upper
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure167", "Moulder Mansion",
			new String[] { "enter combat", "mysticality substats", "HP & MP & Bats in the Belfry" } ),

		// Midst the Pallor of the Parlor
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure168", "Moulder Mansion",
			new String[] { "moxie substats", "Spirit of Alph -> Feelin' Philosophical", "Rat-Faced -> Unusual Fashion Sense" } ),

		// A Few Chintz Curtains, Some Throw Pillows, It
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure169", "Moulder Mansion",
			new String[] { "Night Vision -> flask of Amontillado", "muscle substats", "Dancing Prowess -> fancy ball mask" },
			new String[] { "2661", null, "2662" } ),

		// La Vie Boheme
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure170", "Rogue Windmill",
			new String[] { "HP & Rat-Faced", "enter combat", "moxie substats" } ),

		// Backstage at the Rogue Windmill
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure171", "Rogue Windmill",
			new String[] { "Bats in the Belfry -> No Vertigo", "muscle substats", "Spirit of Alph -> Dancing Prowess" } ),

		// Up in the Hippo Room
		new ChoiceAdventure(
			"Wormwood", "choiceAdventure172", "Rogue Windmill",
			new String[] { "Good with the Ladies -> Can-Can skirt", "Feelin' Philosophical -> not-a-pipe", "mysticality substats" },
			new String[] { "2663", "2660", null } ),

		// Choice 173-176 are unknown

		// The Blackberry Cobbler
		new ChoiceAdventure(
			"Woods", "choiceAdventure177", "Black Forest",
			new String[] { "blackberry slippers", "blackberry moccasins", "blackberry combat boots", "skip adventure" },
			new String[] { "2705", "2706", "2707", null } ),

		// Hammering the Armory
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure178", "Airship Shirt",
			new String[] { "bronze breastplate", "skip adventure" },
			new String[] { "2126", null } ),

		// A Pre-War Dresser Drawer, Pa!
		new ChoiceAdventure(
			"Plains", "choiceAdventure180", "Palindome Shirt",
			new String[] { "Ye Olde Navy Fleece", "skip adventure" },
			new String[] { "2125", null } ),

		// Chieftain of the Flies
		new ChoiceAdventure(
			"Island", "choiceAdventure181", "Frat House (Stone Age)",
			new String[] { "around the world", "skip adventure" },
			new String[] { "1634", "1633" } ),

		// Random Lack of an Encounter
		new ChoiceAdventure(
			"Beanstalk", "choiceAdventure182", "Fantasy Airship",
			new String[] { "enter combat", "Penultimate Fantasy chest", "stats" },
			new String[] { null, "604", null } ),

		// That Explains All The Eyepatches
		// Dynamically calculate options based on mainstat
		new ChoiceAdventure(
			"Island", "choiceAdventure184", "Barrrney's Barrr",
			null ),

		// Yes, You're a Rock Starrr
		new ChoiceAdventure(
			"Island", "choiceAdventure185", "Barrrney's Barrr",
			new String[] { "base booze", "mixed drinks", "stats" } ),

		// A Test of Testarrrsterone
		new ChoiceAdventure(
			"Island", "choiceAdventure186", "Barrrney's Barrr",
			new String[] { "stats", "drunkenness and stats", "moxie" } ),

		// Choice 187 is Arrr You Man Enough?
		// Choice 188 is The Infiltrationist
		// Choice 189 is O Cap'm, My Cap'm

		// Choice 190 is unknown

		// Chatterboxing
		new ChoiceAdventure(
			"Island", "choiceAdventure191", "F'c'le",
			new String[] { "moxie substats", "lose hp", "muscle substats", "mysticality substats" } ),

                // Choice 209 is Timbarrrr!
	};

	static
	{
		Arrays.sort( ChoiceManager.CHOICE_ADVS );
	}

	// We choose to not make some choice adventures configurable, but we
	// want to provide spoilers in the browser for them.

	public static final ChoiceAdventure[] CHOICE_ADV_SPOILERS =
	{
		// Denim Axes Examined
		new ChoiceAdventure(
			"choiceAdventure2", "Palindome",
			new String[] { "denim axe", "skip adventure" },
			new String[] { "499", "292" } ),

		// The Oracle Will See You Now
		new ChoiceAdventure(
			"choiceAdventure3", "Teleportitis",
			new String[] { "skip adventure", "randomly sink 100 meat", "make plus sign usable" } ),

		// Darker Than Dark
		new ChoiceAdventure(
			"choiceAdventure6", "Gravy Barrow",
			new String[] { "fight the fairy queen", "skip adventure" } ),

		// How Depressing -> Self Explanatory
		new ChoiceAdventure(
			"choiceAdventure7", "Gravy Barrow",
			new String[] { "fight the fairy queen", "skip adventure" } ),

		// On the Verge of a Dirge -> Self Explanatory
		new ChoiceAdventure(
			"choiceAdventure8", "Gravy Barrow",
			new String[] { "enter the chamber", "enter the chamber", "enter the chamber" } ),

		// Wheel In the Sky Keep on Turning: Muscle Position
		new ChoiceAdventure(
			"choiceAdventure9", "Castle Wheel",
			new String[] { "Turn to mysticality", "Turn to moxie", "Leave at muscle" } ),

		// Wheel In the Sky Keep on Turning: Mysticality Position
		new ChoiceAdventure(
			"choiceAdventure10", "Castle Wheel",
			new String[] { "Turn to Map Quest", "Turn to muscle", "Leave at mysticality" } ),

		// Wheel In the Sky Keep on Turning: Map Quest Position
		new ChoiceAdventure(
			"choiceAdventure11", "Castle Wheel",
			new String[] { "Turn to moxie", "Turn to mysticality", "Leave at map quest" } ),

		// Wheel In the Sky Keep on Turning: Moxie Position
		new ChoiceAdventure(
			"choiceAdventure12", "Castle Wheel",
			new String[] { "Turn to muscle", "Turn to map quest", "Leave at moxie" } ),

		// Ouch! You bump into a door!
		new ChoiceAdventure(
			"choiceAdventure25", "Dungeon of Doom",
			new String[] { "magic lamp", "dead mimic", "skip adventure" },
			new String[] { "1273", "1267", null } ),

		// A Three-Tined Fork
		new ChoiceAdventure(
			"choiceAdventure26", "Spooky Forest",
			new String[] { "muscle classes", "mysticality classes", "moxie classes" } ),

		// Footprints
		new ChoiceAdventure(
			"choiceAdventure27", "Spooky Forest",
			new String[] { KoLCharacter.SEAL_CLUBBER, KoLCharacter.TURTLE_TAMER } ),

		// A Pair of Craters
		new ChoiceAdventure(
			"choiceAdventure28", "Spooky Forest",
			new String[] { KoLCharacter.PASTAMANCER, KoLCharacter.SAUCEROR } ),

		// The Road Less Visible
		new ChoiceAdventure(
			"choiceAdventure29", "Spooky Forest",
			new String[] { KoLCharacter.DISCO_BANDIT, KoLCharacter.ACCORDION_THIEF } ),

		// Maps and Legends
		new ChoiceAdventure(
			"choiceAdventure45", "Spooky Forest",
			new String[] { "Spooky Temple map", "skip adventure", "skip adventure" },
			new String[] { "74", null, null } ),

		// A Journey to the Center of Your Mind -> Self Explanatory

		// Minnesota Incorporeals
		new ChoiceAdventure(
			"choiceAdventure77", "Haunted Billiard Room",
			new String[] { "moxie substats", "other options", "skip adventure" } ),

		// Broken
		new ChoiceAdventure(
			"choiceAdventure78", "Haunted Billiard Room",
			new String[] { "other options", "muscle substats", "skip adventure" } ),

		// A Hustle Here, a Hustle There
		new ChoiceAdventure(
			"choiceAdventure79", "Haunted Billiard Room",
			new String[] { "Spookyraven library key", "mysticality substats", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure(
			"choiceAdventure80", "Haunted Library",
			new String[] { "background history", "cooking recipe", "other options", "skip adventure" } ),

		// Take a Look, it's in a Book!
		new ChoiceAdventure(
			"choiceAdventure81", "Haunted Library",
			new String[] { "gallery quest", "cocktailcrafting recipe", "muscle substats", "skip adventure" } ),

		// History is Fun!
		new ChoiceAdventure(
			"choiceAdventure86", "Haunted Library",
			new String[] { "Spookyraven Chapter 1", "Spookyraven Chapter 2", "Spookyraven Chapter 3" } ),

		// History is Fun!
		new ChoiceAdventure(
			"choiceAdventure87", "Haunted Library",
			new String[] { "Spookyraven Chapter 4", "Spookyraven Chapter 5 (Gallery Quest)", "Spookyraven Chapter 6" } ),

		// Naughty, Naughty
		new ChoiceAdventure(
			"choiceAdventure88", "Haunted Library",
			new String[] { "mysticality substats", "moxie substats", "Fettucini / Scarysauce" } ),

		new ChoiceAdventure(
			"choiceAdventure89", "Haunted Gallery",
			new String[] { "Wolf Knight", "Snake Knight", "Dreams and Lights" } ),

		// Louvre It or Leave It
		new ChoiceAdventure(
			"choiceAdventure91", "Haunted Gallery",
			new String[] { "Enter the Drawing", "skip adventure" } ),

		// At Least It's Not Full Of Trash
		new ChoiceAdventure(
			"choiceAdventure123", "Hidden Temple",
			new String[] { "lose HP", "Unlock Quest Puzzle", "lose HP" } ),

		// No Visible Means of Support
		new ChoiceAdventure(
			"choiceAdventure125", "Hidden Temple",
			new String[] { "lose HP", "lose HP", "Unlock Hidden City" } ),

		// No sir, away!  A papaya war is on!
		new ChoiceAdventure(
			"choiceAdventure127", "Palindome",
			new String[] { "3 papayas", "trade 3 papayas for stats", "stats" },
			new String[] { "498", null, null } ),

		// Do Geese See God?
		new ChoiceAdventure(
			"choiceAdventure129", "Palindome",
			new String[] { "photograph of God", "skip adventure" },
			new String[] { "2259", null } ),

		// Rod Nevada, Vendor
		new ChoiceAdventure(
			"choiceAdventure130", "Palindome",
			new String[] { "hard rock candy", "skip adventure" },
			new String[] { "2260", null } ),

		// Lurking at the Threshold
		new ChoiceAdventure(
			"choiceAdventure152", "Fun House",
			new String[] { "fight the clownlord", "skip adventure" } ),

		// Between a Rock and Some Other Rocks
		new ChoiceAdventure(
			"choiceAdventure162", "Goatlet",
			new String[] { "Open Goatlet", "skip adventure" } ),

	};

	// Some choice adventures have options that cost meat or items

	public static final Object[][] CHOICE_COST =
	{
		// Denim Axes Examined
		{ "2", "1", new AdventureResult( "rubber axe", -1 ) },

		// Finger-Lickin'... Death.
		{ "4", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },
		{ "4", "2", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Under the Knife
		{ "21", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Ouch! You bump into a door!
		{ "25", "1", new AdventureResult( AdventureResult.MEAT, -50 ) },
		{ "25", "2", new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// Have a Heart
		// This trades all vampire hearts for an equal number of
		// bottles of used blood.
		{ "47", "1", new AdventureResult( "vampire heart", 1 ) },

		// Lording Over The Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		{ "72", "1", new AdventureResult( "Spanish fly", 5 ) },

		// No sir, away!  A papaya war is on!
		{ "127", "2", new AdventureResult( "papaya", -3 ) },

		// Do Geese See God?
		{ "129", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Rod Nevada, Vendor
		{ "130", "1", new AdventureResult( AdventureResult.MEAT, -500 ) },

		// Let's Make a Deal!
		{ "132", "1", new AdventureResult( AdventureResult.MEAT, -5000 ) },

		// The Blackberry Cobbler
		{ "177", "1", new AdventureResult( "blackberry", -10 ) },
		{ "177", "2", new AdventureResult( "blackberry", -10 ) },
		{ "177", "3", new AdventureResult( "blackberry", -10 ) },

		// Chieftain of the Flies
		// This trades all Spanish flies for around the worlds,
		// in multiples of 5.  Excess flies are left in inventory.
		{ "181", "1", new AdventureResult( "Spanish fly", 5 ) },

		//  O Cap'm, My Cap'm
		{ "189", "1", new AdventureResult( AdventureResult.MEAT, -977 ) },
	};

	public static final AdventureResult getCost( final String choice, final String decision )
	{
		for ( int i = 0; i < ChoiceManager.CHOICE_COST.length; ++i )
		{
			if ( choice.equals( ChoiceManager.CHOICE_COST[ i ][ 0 ] ) && decision.equals( ChoiceManager.CHOICE_COST[ i ][ 1 ] ) )
			{
				return (AdventureResult) ChoiceManager.CHOICE_COST[ i ][ 2 ];
			}
		}

		return null;
	}

	public static final String[][] choiceSpoilers( final int choice )
	{
		String[][] spoilers;

		// See if spoilers are dynamically generated
		spoilers = ChoiceManager.dynamicChoiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		// Nope. See if it's in the Violet Fog
		spoilers = VioletFogManager.choiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		// Nope. See if it's in the Louvre
		spoilers = LouvreManager.choiceSpoilers( choice );
		if ( spoilers != null )
		{
			return spoilers;
		}

		String option = "choiceAdventure" + String.valueOf( choice );

		// See if this choice is controlled by user option
		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length; ++i )
		{
			if ( ChoiceManager.CHOICE_ADVS[ i ].getSetting().equals( option ) )
			{
				return ChoiceManager.CHOICE_ADVS[ i ].getSpoilers();
			}
		}

		// Nope. See if we know this choice
		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length; ++i )
		{
			if ( ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSetting().equals( option ) )
			{
				return ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSpoilers();
			}
		}

		// Unknown choice
		return null;
	}

	private static final String[][] dynamicChoiceSpoilers( final int choice )
	{
		String[][] result;
		switch ( choice )
		{
		case 184:
			// That Explains All The Eyepatches
			result = new String[ 4 ][];

			// The choice option is the first element
			result[ 0 ] = new String[ 1 ];
			result[ 0 ][ 0 ] = "choiceAdventure184";

			// The name of the choice is second element
			result[ 1 ] = new String[ 1 ];
			result[ 1 ][ 0 ] = "Barrrney's Barrr";

			// An array of choice spoilers is the third element
			result[ 2 ] = ChoiceManager.dynamicChoiceOptions( choice );

			// A parallel array of items is the fourth element
			result[ 3 ] = new String[ 3 ];

			for ( int i = 0; i < 3; ++i )
			{
				if ( result[ 2 ][ i ].equals( "shot of rotgut" ) )
				{
					result[ 3 ][ i ] = "2948";
				}
			}

			return result;

		case 187:
			// Arrr You Man Enough?
			result = new String[ 3 ][];

			// The choice option is the first element
			result[ 0 ] = new String[ 1 ];
			result[ 0 ][ 0 ] = "choiceAdventure187";

			// The name of the choice is second element
			result[ 1 ] = new String[ 1 ];
			result[ 1 ][ 0 ] = "Barrrney's Barrr";

			// An array of choice spoilers is the third element
			result[ 2 ] = ChoiceManager.dynamicChoiceOptions( choice );

			return result;
		}
		return null;
	}

	private static final String[] dynamicChoiceOptions( final int choice )
	{
		String[] result;
		switch ( choice )
		{
		case 184:
			// That Explains All The Eyepatches
			result = new String[ 3 ];

			// The choices are all stat based.
			//
			// The are definitely NOT based on buffed stat.	 It
			// could be based on base stat or on character class -
			// which are the same, bar great effort
			//
			// For now, assume base stat.

			int mus = KoLCharacter.getBaseMuscle();
			int mys = KoLCharacter.getBaseMysticality();
			int mox = KoLCharacter.getBaseMoxie();
			int stat;

			if ( mus > mys )
			{
				stat = mus > mox ? KoLConstants.MUSCLE : KoLConstants.MOXIE;
			}
			else
			{
				stat = mys > mox ? KoLConstants.MYSTICALITY : KoLConstants.MOXIE;
			}

			// Mus: combat, shot of rotgut (2948), drunkenness
			// Mys: drunkenness, shot of rotgut (2948), shot of rotgut (2948)
			// Mox: combat, drunkenness, shot of rotgut (2948)

			result[ 0 ] = stat == KoLConstants.MYSTICALITY ? "drunkenness and stats" : "enter combat";
			result[ 1 ] = stat == KoLConstants.MOXIE ? "drunkenness and stats" : "shot of rotgut";
			result[ 2 ] = stat == KoLConstants.MUSCLE ? "drunkenness and stats" : "shot of rotgut";
			return result;

		case 187:
			// Arrr You Man Enough?

			result = new String[ 2 ];
			float odds = FightRequest.pirateInsultOdds() * 100.0f;

			result[ 0 ] = KoLConstants.FLOAT_FORMAT.format( odds ) + "% chance of winning";
			result[ 1 ] = odds == 100.0f ? "Oh come on. Do it!" : "Try later";
			return result;
		}
		return null;
	}

	public static final String[] dynamicChoiceOptions( final String option )
	{
		if ( !option.startsWith( "choiceAdventure" ) )
		{
			return null;
		}
		int choice = StringUtilities.parseInt( option.substring( 15 ) );
		return ChoiceManager.dynamicChoiceOptions( choice );
	}

	public static final String choiceSpoiler( final int choice, final int decision, final String[] spoilers )
	{
		switch ( choice )
		{
		case 105:
			// Having a Medicine Ball
			if ( decision == 2 )
			{
				KoLCharacter.ensureUpdatedGuyMadeOfBees();
				boolean defeated = Preferences.getBoolean( "guyMadeOfBeesDefeated" );
				if ( defeated )
				{
					return "guy made of bees: defeated";
				}
				return "guy made of bees: called " + Preferences.getString( "guyMadeOfBeesCount" ) + " times";
			}
			break;
		}
		return spoilers[ decision ];
	}

	public static final void processChoiceAdventure()
	{
		ChoiceManager.processChoiceAdventure( ChoiceManager.CHOICE_HANDLER );
	}

	/**
	 * Utility method which notifies thethat it needs to process the given choice adventure.
	 */

	public static final void processChoiceAdventure( final GenericRequest request )
	{
		if ( GenericRequest.passwordHash.equals( "" ) )
		{
			return;
		}

		// You can no longer simply ignore a choice adventure.	One of
		// the options may have that effect, but we must at least run
		// choice.php to find out which choice it is.

		ResultProcessor.processResult( new AdventureResult( AdventureResult.CHOICE, 1 ) );
		request.constructURLString( "choice.php" );
		request.run();

		if ( request.responseCode == 302 )
		{
			return;
		}

		String choice = null;
		String option = null;
		String decision = null;

		for ( int stepCount = 0; request.responseText.indexOf( "choice.php" ) != -1; ++stepCount )
		{
			// Slight delay before each choice is made

			Matcher choiceMatcher = ChoiceManager.CHOICE_PATTERN.matcher( request.responseText );

			if ( !choiceMatcher.find() )
			{
				// choice.php did not offer us any choices. This would
				// be a bug in KoL itself. Bail now and let the user
				// finish by hand.

				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Encountered choice adventure with no choices." );
				request.showInBrowser( true );
				return;
			}

			choice = choiceMatcher.group( 1 );
			option = "choiceAdventure" + choice;
			decision = Preferences.getString( option );

			// Certain choices should always be taken.  These
			// choices are handled here.

			if ( choice.equals( "7" ) )
			{
				decision = "1";
			}

			// If this happens to be adventure 26 or 27,
			// check against the player's conditions.

			if ( ( choice.equals( "26" ) || choice.equals( "27" ) ) && !KoLConstants.conditions.isEmpty() )
			{
				for ( int i = 0; i < 12; ++i )
				{
					if ( AdventureDatabase.WOODS_ITEMS[ i ].getCount( KoLConstants.conditions ) > 0 )
					{
						decision = choice.equals( "26" ) ? String.valueOf( i / 4 + 1 ) : String.valueOf( i % 4 / 2 + 1 );
					}
				}
			}

			// If the player is looking for the ballroom key,
			// then update their preferences so that KoLmafia
			// automatically switches things for them.

			if ( choice.equals( "85" ) )
			{
				if ( !KoLConstants.inventory.contains( ChoiceManager.BALLROOM_KEY ) )
				{
					Preferences.setString( option, decision.equals( "1" ) ? "2" : "1" );
				}
				else
				{
					for ( int i = 0; i < ChoiceManager.MISTRESS_ITEMS.length; ++i )
					{
						if ( KoLConstants.conditions.contains( ChoiceManager.MISTRESS_ITEMS[ i ] ) )
						{
							decision = "3";
						}
					}
				}
			}

			// Auto-skip the goatlet adventure if you're not wearing
			// the mining outfit so it can be tried again later.

			if ( choice.equals( "162" ) && !EquipmentManager.isWearingOutfit( 8 ) )
			{
				decision = "2";
			}

			// Sometimes, the choice adventure for the louvre
			// loses track of whether to ignore the louvre or not.

			if ( choice.equals( "91" ) )
			{
				decision = Preferences.getInteger( "louvreDesiredGoal" ) != 0 ? "1" : "2";
			}

			// If there is no setting which determines the
			// decision, see if it's in the violet fog

			if ( decision.equals( "" ) )
			{
				decision = VioletFogManager.handleChoice( choice );
			}

			// If there is no setting which determines the
			// decision, see if it's in the Louvre

			if ( decision.equals( "" ) )
			{
				decision = LouvreManager.handleChoice( choice, stepCount );
			}

			// If there is currently no setting which determines the
			// decision, give an error and bail.

			if ( decision.equals( "" ) )
			{
				KoLmafia.updateDisplay( KoLConstants.ABORT_STATE, "Unsupported choice adventure #" + choice );
				request.showInBrowser( true );
				StaticEntity.printRequestData( request );
				return;
			}

			boolean willIgnore = false;

			// If the user wants to ignore this specific choice or all
			// choices, see if this choice is ignorable.

			if ( choice.equals( "80" ) )
			{
				willIgnore = true;

				if ( decision.equals( "99" ) && Preferences.getInteger( "lastSecondFloorUnlock" ) == KoLCharacter.getAscensions() )
				{
					decision = "4";
				}
			}
			else if ( choice.equals( "81" ) )
			{
				willIgnore = true;

				// If we've already unlocked the gallery, try
				// to unlock the second floor.
				if ( decision.equals( "1" ) && Preferences.getInteger( "lastGalleryUnlock" ) == KoLCharacter.getAscensions() )
				{
					decision = "99";
				}

				// If we've already unlocked the second floor,
				// ignore this choice adventure.
				if ( decision.equals( "99" ) && Preferences.getInteger( "lastSecondFloorUnlock" ) == KoLCharacter.getAscensions() )
				{
					decision = "4";
				}
			}

			// But first, handle the maidens adventure in a less random
			// fashion that's actually useful.

			else if ( choice.equals( "89" ) )
			{
				willIgnore = true;

				switch ( StringUtilities.parseInt( decision ) )
				{
				case 0:
					decision = String.valueOf( KoLConstants.RNG.nextInt( 2 ) + 1 );
					break;
				case 1:
				case 2:
					break;
				case 3:
					decision =
						KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? String.valueOf( KoLConstants.RNG.nextInt( 2 ) + 1 ) : "3";
					break;
				case 4:
					decision = KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? "1" : "3";
					break;
				case 5:
					decision = KoLConstants.activeEffects.contains( ChoiceManager.MAIDEN_EFFECT ) ? "2" : "3";
					break;
				}
			}

			else if ( choice.equals( "127" ) )
			{
				willIgnore = true;

				switch ( StringUtilities.parseInt( decision ) )
				{
				case 1:
				case 2:
				case 3:
					break;
				case 4:
					decision = ChoiceManager.PAPAYA.getCount( KoLConstants.inventory ) >= 3 ? "2" : "1";
					break;
				case 5:
					decision = ChoiceManager.PAPAYA.getCount( KoLConstants.inventory ) >= 3 ? "2" : "3";
					break;
				}
			}

			else if ( choice.equals( "161" ) )
			{
				decision = "1";

				for ( int i = 2566; i <= 2568; ++i )
				{
					AdventureResult item = new AdventureResult( i, 1 );
					if ( !KoLConstants.inventory.contains( item ) )
					{
						decision = "4";
					}
				}
			}

			// Always change the option whenever it's not an ignore option
			// and remember to store the result.

			if ( !willIgnore )
			{
				decision = ChoiceManager.pickOutfitChoice( option, decision );
			}

			request.clearDataFields();

			request.addFormField( "pwd" );
			request.addFormField( "whichchoice", choice );
			request.addFormField( "option", decision );

			request.run();
		}

		if ( choice != null && KoLmafia.isAdventuring() )
		{
			if ( choice.equals( "112" ) && decision.equals( "1" ) )
			{
				InventoryManager.retrieveItem( new AdventureResult( 2184, 1 ) );
			}

			if ( choice.equals( "162" ) && !EquipmentManager.isWearingOutfit( 8 ) )
			{
				CouncilFrame.unlockGoatlet();
			}
		}
	}

	private static final String pickOutfitChoice( final String option, final String decision )
	{
		// Find the options for the choice we've encountered

		boolean matchFound = false;
		String[] possibleDecisions = null;
		String[] possibleDecisionSpoilers = null;

		for ( int i = 0; i < ChoiceManager.CHOICE_ADVS.length && !matchFound; ++i )
		{
			if ( ChoiceManager.CHOICE_ADVS[ i ].getSetting().equals( option ) )
			{
				matchFound = true;
				possibleDecisions = ChoiceManager.CHOICE_ADVS[ i ].getItems();
				possibleDecisionSpoilers = ChoiceManager.CHOICE_ADVS[ i ].getOptions();
			}
		}

		for ( int i = 0; i < ChoiceManager.CHOICE_ADV_SPOILERS.length && !matchFound; ++i )
		{
			if ( ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getSetting().equals( option ) )
			{
				matchFound = true;
				possibleDecisions = ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getItems();
				possibleDecisionSpoilers = ChoiceManager.CHOICE_ADV_SPOILERS[ i ].getOptions();
			}
		}

		// If it's not in the table (the castle wheel, for example) or
		// isn't an outfit completion choice, return the player's
		// chosen decision.

		if ( possibleDecisionSpoilers == null )
		{
			return decision.equals( "0" ) ? "1" : decision;
		}

		// Choose an item in the conditions first, if it's available.
		// This allows conditions to override existing choices.

		if ( possibleDecisions != null )
		{
			for ( int i = 0; i < possibleDecisions.length; ++i )
			{
				if ( possibleDecisions[ i ] == null )
				{
					continue;
				}

				AdventureResult item = new AdventureResult( StringUtilities.parseInt( possibleDecisions[ i ] ), 1 );
				if ( KoLConstants.conditions.contains( item ) )
				{
					return String.valueOf( i + 1 );
				}

				if ( possibleDecisions.length < StringUtilities.parseInt( decision ) && !InventoryManager.hasItem( item ) )
				{
					return String.valueOf( i + 1 );
				}
			}
		}

		if ( possibleDecisions == null )
		{
			return decision.equals( "0" ) ? "1" : decision;
		}

		// If this is an ignore decision, then go ahead and ignore
		// the choice adventure

		int decisionIndex = StringUtilities.parseInt( decision ) - 1;
		if ( possibleDecisions.length < possibleDecisionSpoilers.length && possibleDecisionSpoilers[ decisionIndex ].equals( "skip adventure" ) )
		{
			return decision;
		}

		// If no item is found in the conditions list, and the player
		// has a non-ignore decision, go ahead and use it.

		if ( !decision.equals( "0" ) && decisionIndex < possibleDecisions.length )
		{
			return decision;
		}

		// Choose a null choice if no conditions match what you're
		// trying to look for.

		for ( int i = 0; i < possibleDecisions.length; ++i )
		{
			if ( possibleDecisions[ i ] == null )
			{
				return String.valueOf( i + 1 );
			}
		}

		// If they have everything and it's an ignore choice, then use
		// the first choice no matter what.

		return "1";
	}
}
