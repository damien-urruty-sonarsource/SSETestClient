package com.sonarsource;

import java.net.ProxySelector;
import java.nio.CharBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.auth.SystemDefaultCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.SystemDefaultRoutePlanner;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.nio.support.BasicRequestProducer;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.util.Timeout;

public class ApacheHttpClient {
  private static final Timeout CONNECTION_TIMEOUT = Timeout.ofSeconds(30);
  private static final Timeout RESPONSE_TIMEOUT = Timeout.ofMinutes(10);
  public static final String DATA_PREFIX = "data: ";
  public static final String DATA_SUFFIX = "\n\n";

  private final CloseableHttpAsyncClient client;

  public ApacheHttpClient() throws NoSuchAlgorithmException {
    client = createClient();
    client.start();
  }

  public void getStreamed(String url, Consumer<String> messageConsumer) throws ExecutionException, InterruptedException {
    SimpleHttpRequest request = SimpleHttpRequests.get(url);
    request.setHeader("Accept", "text/event-stream");

    final Future<Void> future = client.execute(
      new BasicRequestProducer(request, null),
      new AbstractCharResponseConsumer<>() {

        @Override
        protected void start(
          final HttpResponse response,
          final ContentType contentType) {
          System.out.println(request + "->" + new StatusLine(response));
        }

        @Override
        protected int capacityIncrement() {
          return Integer.MAX_VALUE;
        }

        @Override
        protected void data(final CharBuffer data, final boolean endOfStream) {
          String receivedData = data.toString();
          if (isValidEvent(receivedData)) {
            String withoutPrefix = receivedData.substring(DATA_PREFIX.length());
            String payload = withoutPrefix.substring(0, withoutPrefix.length() - 2);
            messageConsumer.accept(payload);
          } else {
            System.out.println("Data format invalid: " + receivedData);
          }
          if (endOfStream) {
            // should we close something ?
          }
        }

        private boolean isValidEvent(String receivedData) {
          return receivedData.startsWith(DATA_PREFIX) && receivedData.endsWith(DATA_SUFFIX);
        }

        @Override
        protected Void buildResult() {
          return null;
        }

        @Override
        public void failed(final Exception cause) {
          // should we close something ?
        }

        @Override
        public void releaseResources() {
          // should we close something ?
        }

      }, null);
    future.get();
  }

  private CloseableHttpAsyncClient createClient() throws NoSuchAlgorithmException {
    return HttpAsyncClients.custom()
      .setConnectionManager(
        PoolingAsyncClientConnectionManagerBuilder.create()
          .setTlsStrategy(
            ClientTlsStrategyBuilder.create()
//                .setSslContext(CertificateManager.getInstance().sslContext)
              .setSslContext(SSLContext.getDefault())
              .setTlsDetailsFactory(c -> new TlsDetails(c.getSession(), c.getApplicationProtocol()))
              .build())
          .build()
      )
      .setUserAgent("SonarLint IntelliJ ")

      // proxy settings
//    .setRoutePlanner(SystemDefaultRoutePlanner(CommonProxy.getInstance()))
      .setRoutePlanner(new SystemDefaultRoutePlanner(ProxySelector.getDefault()))
      .setDefaultCredentialsProvider(new SystemDefaultCredentialsProvider())

      .setDefaultRequestConfig(
        RequestConfig.copy(RequestConfig.DEFAULT)
          .setConnectionRequestTimeout(CONNECTION_TIMEOUT)
          .setResponseTimeout(RESPONSE_TIMEOUT)
          .build()
      )
      .build();
  }
}
