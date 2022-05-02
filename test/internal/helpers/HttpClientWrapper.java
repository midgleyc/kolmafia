package internal.helpers;

import internal.network.FakeHttpClientBuilder;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.utilities.HttpUtilities;

public class HttpClientWrapper {

  public static FakeHttpClientBuilder fakeClientBuilder = new FakeHttpClientBuilder();

  public static List<HttpRequest> getRequests() {
    return fakeClientBuilder.client.getRequests();
  }

  public static HttpRequest getLastRequest() {
    return fakeClientBuilder.client.getLastRequest();
  }

  public static void setResponse(String target, String response) {
    fakeClientBuilder.client.setResponse((req) -> req.uri().toString().endsWith(target), 200, response);
  }

  public static void familiarRequestSucceeds(String famName) {
    fakeClientBuilder.client.setResponse((req) -> req.uri().toString().endsWith("familiar.php"), 200, "You take " + famName + " with you.");
  }

  public static void setupFakeClient() {
    GenericRequest.sessionId = "real"; // do "send" requests
    HttpUtilities.setClientBuilder(() -> fakeClientBuilder);
    GenericRequest.resetClient();
    fakeClientBuilder.client.clear();
  }
}
