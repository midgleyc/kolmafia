package net.sourceforge.kolmafia.request.concoction.shop;

import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;

public class SliemceRequest extends CreateItemRequest {
  public SliemceRequest(final Concoction conc) {
    super("shop.php", conc);

    this.addFormField("whichshop", "voteslime");
    this.addFormField("action", "buyitem");
    int row = ConcoctionPool.idToRow(this.getItemId());
    this.addFormField("whichrow", String.valueOf(row));
  }

  @Override
  public void run() {
    // Attempting to make the ingredients will pull the
    // needed items from the closet if they are missing.

    if (!this.makeIngredients()) {
      return;
    }

    KoLmafia.updateDisplay("Creating " + this.getQuantityNeeded() + " " + this.getName() + "...");
    this.addFormField("quantity", String.valueOf(this.getQuantityNeeded()));
    super.run();
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    String responseText = this.responseText;

    if (urlString.contains("action=buyitem") && !responseText.contains("You acquire")) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Mad Sliemce crafting was unsuccessful.");
      return;
    }

    SliemceRequest.parseResponse(urlString, responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=voteslime")) {
      return;
    }

    NPCPurchaseRequest.parseShopRowResponse(urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=voteslime")) {
      return false;
    }

    return NPCPurchaseRequest.registerShopRowRequest(urlString);
  }
}
