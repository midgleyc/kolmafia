package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.HttpClientWrapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HeistCommandTest extends AbstractCommandTestBase {

  public static String heistText;

  @BeforeAll
  public static void setup() throws IOException {
    heistText = Files.readString(Paths.get("request/test_heist_command.html"));
  }

  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");

    HttpClientWrapper.setupFakeClient();
    HttpClientWrapper.setResponse("main.php", heistText);
    HttpClientWrapper.familiarRequestSucceeds("Cat Burglar");
  }

  public HeistCommandTest() {
    this.command = "heist";
  }

  private static void setCatBurglar() {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.CAT_BURGLAR, 1);
    KoLCharacter.setFamiliar(familiar);
  }

  @Test
  void mustHaveCatBurglar() {
    KoLCharacter.familiars.clear();
    String output = execute("");

    assertThat(output, containsString("You don't have a Cat Burglar"));
    assertContinueState();
  }

  @Test
  void mustSpecifyValidItem() {
    setCatBurglar();
    String output = execute("an invalid item");

    assertThat(output, containsString("What item is an invalid item?"));
    assertErrorState();
  }

  @Test
  void parsesHeistPage() {
    setCatBurglar();
    String output = execute("");

    assertThat(output, containsString("You have 42 heists."));
    assertThat(output, containsString("From  bigface:"));
    assertThat(output, containsString("From a jock:"));
    assertThat(output, containsString("From a burnout:"));
    assertContinueState();
  }

  @Test
  void doesNotHeistInvalidItem() {
    setCatBurglar();
    String output = execute("334 scroll");

    assertThat(output, containsString("Could not find 334 scroll to heist"));
    assertErrorState();
  }

  @Test
  void heistsValidItemExact() {
    setCatBurglar();
    String output = execute("ratty knitted cap");

    assertThat(output, containsString("Heisted ratty knitted cap"));
    assertContinueState();
  }

  @Test
  void heistsValidItem() {
    setCatBurglar();
    String output = execute("Purple Beast");

    assertThat(output, containsString("Heisted Purple Beast energy drink"));
    assertContinueState();
  }

  @Test
  void heistsValidItemWithQuotes() {
    setCatBurglar();
    String output = execute("\"meat\" stick");

    assertThat(output, containsString("Heisted &quot;meat&quot; stick"));
    assertContinueState();
  }

  @Test
  void heistsMultipleValidItem() {
    setCatBurglar();
    String output = execute("13 Purple Beast");

    assertThat(output, containsString("Heisted 13 Purple Beast energy drinks"));
    assertContinueState();
  }
}
