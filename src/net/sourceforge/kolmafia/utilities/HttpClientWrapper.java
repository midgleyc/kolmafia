package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;

public class HttpClientWrapper {
  private final HttpClient.Builder clientBuilder;
  private final HttpRequest.Builder requestBuilder;

  public HttpClientWrapper() {
    this(HttpClient.newBuilder(), HttpRequest.newBuilder());
  }

  public HttpClientWrapper(HttpClient.Builder clientBuilder, HttpRequest.Builder requestBuilder) {
    this.clientBuilder = clientBuilder;
    this.requestBuilder = requestBuilder;
  }

  public Map<String, List<String>> getHeaders() {
    return this.requestBuilder.build().headers().map();
  }

  public String getRequestMethod() {
    return this.requestBuilder.build().method();
  }

  public HttpClientWrapper uri(URI uri) {
    this.requestBuilder.uri(uri);
    return this;
  }

  public HttpClientWrapper setIfModifiedSince(long since) {
    var header = StringUtilities.formatDate(since);
    this.requestBuilder.setHeader("If-Modified-Since", header);
    return this;
  }

  public HttpClientWrapper addRequestProperty(String key, String value) {
    this.requestBuilder.header(key, value);
    return this;
  }

  public HttpClientWrapper setRequestProperty(String key, String value) {
    this.requestBuilder.setHeader(key, value);
    return this;
  }

  public HttpClientWrapper setupPostData(byte[] data) {
    this.requestBuilder.POST(BodyPublishers.ofByteArray(data));
    return this;
  }

  public HttpResponse<InputStream> sendForInputStream() throws IOException, InterruptedException {
    var client = this.clientBuilder.build();
    var request = this.requestBuilder.build();
    return client.send(request, BodyHandlers.ofInputStream());
  }
}
