package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class HttpUtilities {
  private HttpUtilities() {}

  @FunctionalInterface
  public interface ConnectionFactory {
    HttpURLConnection openConnection(URL url) throws IOException;
  }

  @FunctionalInterface
  public interface RequestFactory {
    HttpClientWrapper getHttpWrapper(URI uri);
  }

  private static ConnectionFactory factory = (URL url) -> (HttpURLConnection) url.openConnection();

  private static RequestFactory wrapperFactory =
      (URI uri) -> {
        var wrapper = new HttpClientWrapper();
        wrapper.uri(uri);
        return wrapper;
      };

  // Injects custom URL handling logic, especially in tests.
  public static void setOpen(ConnectionFactory function) {
    HttpUtilities.factory = function;
  }

  // Injects custom URL handling logic, especially in tests.
  public static void setRequest(RequestFactory function) {
    HttpUtilities.wrapperFactory = function;
  }

  public static HttpURLConnection openConnection(URL url) throws IOException {
    return HttpUtilities.factory.openConnection(url);
  }

  public static HttpClientWrapper getHttpWrapper(URI uri) {
    return HttpUtilities.wrapperFactory.getHttpWrapper(uri);
  }
}
