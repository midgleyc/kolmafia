package internal.network;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Function;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClient extends HttpClient {

  private final List<HttpRequest> requests = new ArrayList<>();

  private static class Response {
    public int responseCode;
    public String responseBody;

    public Response(int responseCode, String responseBody) {
      this.responseCode = responseCode;
      this.responseBody = responseBody;
    }
  }

  private final Map<Function<HttpRequest, Boolean>, Response> responses = new HashMap<>();

  public void setResponse(int responseCode, String response) {
    setResponse((r) -> true, responseCode, response);
  }

  public void setResponse(Function<HttpRequest, Boolean> requestMatcher, int responseCode, String response) {
    responses.put(requestMatcher, new Response(responseCode, response));
  }

  private Response getResponse(HttpRequest request) {
    for (var entry : responses.entrySet()) {
      if (entry.getKey().apply(request)) {
        return entry.getValue();
      }
    }
    return new Response(0, "");
  }

  public List<HttpRequest> getRequests() {
    return requests;
  }

  public HttpRequest getLastRequest() {
    if (requests.size() == 0) return null;

    return requests.get(requests.size() - 1);
  }

  public void clear() {
    this.requests.clear();
    this.responses.clear();
  }

  @Override
  public Optional<CookieHandler> cookieHandler() {
    return Optional.empty();
  }

  @Override
  public Optional<Duration> connectTimeout() {
    return Optional.empty();
  }

  @Override
  public Redirect followRedirects() {
    return null;
  }

  @Override
  public Optional<ProxySelector> proxy() {
    return Optional.empty();
  }

  @Override
  public SSLContext sslContext() {
    return null;
  }

  @Override
  public SSLParameters sslParameters() {
    return null;
  }

  @Override
  public Optional<Authenticator> authenticator() {
    return Optional.empty();
  }

  @Override
  public Version version() {
    return null;
  }

  @Override
  public Optional<Executor> executor() {
    return Optional.empty();
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
      throws IOException, InterruptedException {
    this.requests.add(request);

    T body;

    var response = getResponse(request);

    var subscriber = responseBodyHandler.apply(new ResponseInfoImpl(response.responseCode));
    var publisher = new SubmissionPublisher<List<ByteBuffer>>();
    publisher.subscribe(subscriber);
    publisher.submit(List.of(ByteBuffer.wrap(response.responseBody.getBytes(StandardCharsets.UTF_8))));
    publisher.close();
    try {
      body = subscriber.getBody().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      body = null;
    }
    return new FakeHttpResponse<>(response.responseCode, body);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request, BodyHandler<T> responseBodyHandler) {
    return null;
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request,
      BodyHandler<T> responseBodyHandler,
      PushPromiseHandler<T> pushPromiseHandler) {
    return null;
  }

  static class ResponseInfoImpl implements ResponseInfo {

    int statusCode;

    public ResponseInfoImpl(int statusCode) {
      this.statusCode = statusCode;
    }

    @Override
    public int statusCode() {
      return statusCode;
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(new HashMap<>(), (x, y) -> true);
    }

    @Override
    public Version version() {
      return Version.HTTP_1_1;
    }
  }
}
