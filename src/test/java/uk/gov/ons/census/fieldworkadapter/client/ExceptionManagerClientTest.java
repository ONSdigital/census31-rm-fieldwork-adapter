package uk.gov.ons.census.fieldworkadapter.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.ons.census.fieldworkadapter.model.dto.ExceptionReportResponse;
import uk.gov.ons.census.fieldworkadapter.model.dto.SkippedMessage;

class ExceptionManagerClientTest {

  private final ExceptionManagerClient underTest = new ExceptionManagerClient();

  private HttpServer httpServer;
  private final AtomicReference<String> lastPath = new AtomicReference<>();
  private final AtomicReference<String> lastBody = new AtomicReference<>();

  @BeforeEach
  void setUp() throws Exception {
    httpServer = HttpServer.create(new InetSocketAddress(0), 0);
    httpServer.createContext(
        "/",
        exchange -> {
          lastPath.set(exchange.getRequestURI().getPath());
          lastBody.set(readBody(exchange));

          if ("/reportexception".equals(exchange.getRequestURI().getPath())) {
            writeResponse(exchange, "{\"peek\":true,\"logIt\":false,\"skipIt\":true}");
            return;
          }

          writeResponse(exchange, "{}");
        });
    httpServer.start();

    ReflectionTestUtils.setField(underTest, "scheme", "http");
    ReflectionTestUtils.setField(underTest, "host", "localhost");
    ReflectionTestUtils.setField(
        underTest, "port", String.valueOf(httpServer.getAddress().getPort()));
  }

  @AfterEach
  void tearDown() {
    httpServer.stop(0);
  }

  @Test
  void shouldPostExceptionReportAndMapResponse() {
    RuntimeException cause = new RuntimeException("exception report test");

    ExceptionReportResponse response =
        underTest.reportException("hash-1", "service-a", "subscription-a", cause, "stack-root");

    assertThat(lastPath.get()).isEqualTo("/reportexception");
    assertThat(lastBody.get()).contains("\"messageHash\":\"hash-1\"");
    assertThat(lastBody.get()).contains("\"service\":\"service-a\"");
    assertThat(lastBody.get()).contains("\"subscription\":\"subscription-a\"");
    assertThat(lastBody.get()).contains("\"exceptionClass\":\"java.lang.RuntimeException\"");
    assertThat(lastBody.get()).contains("\"exceptionMessage\":\"exception report test\"");
    assertThat(lastBody.get()).contains("\"exceptionRootCause\":\"stack-root\"");

    assertThat(response.isPeek()).isTrue();
    assertThat(response.isLogIt()).isFalse();
    assertThat(response.isSkipIt()).isTrue();
  }

  @Test
  void shouldPostPeekReply() {
    underTest.respondToPeek("hash-2", "payload".getBytes(StandardCharsets.UTF_8));

    assertThat(lastPath.get()).isEqualTo("/peekreply");
    assertThat(lastBody.get()).contains("\"messageHash\":\"hash-2\"");
    assertThat(lastBody.get()).contains("\"messagePayload\"");
  }

  @Test
  void shouldStoreSkippedMessage() {
    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash("hash-3");
    skippedMessage.setService("service-b");
    skippedMessage.setSubscription("subscription-b");

    underTest.storeMessageBeforeSkipping(skippedMessage);

    assertThat(lastPath.get()).isEqualTo("/storeskippedmessage");
    assertThat(lastBody.get()).contains("\"messageHash\":\"hash-3\"");
    assertThat(lastBody.get()).contains("\"service\":\"service-b\"");
    assertThat(lastBody.get()).contains("\"subscription\":\"subscription-b\"");
  }

  private String readBody(HttpExchange exchange) throws IOException {
    InputStream requestBody = exchange.getRequestBody();
    return new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);
  }

  private void writeResponse(HttpExchange exchange, String responseBody) throws IOException {
    byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    exchange.sendResponseHeaders(200, response.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(response);
    }
  }
}
