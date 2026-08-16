package uk.gov.ons.census.fieldworkadapter.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExceptionDtosTest {

  @Test
  void shouldPopulateExceptionReportFields() {
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("hash");
    exceptionReport.setService("service");
    exceptionReport.setSubscription("subscription");
    exceptionReport.setExceptionClass("clazz");
    exceptionReport.setExceptionMessage("message");
    exceptionReport.setExceptionRootCause("root");

    assertThat(exceptionReport.getMessageHash()).isEqualTo("hash");
    assertThat(exceptionReport.getService()).isEqualTo("service");
    assertThat(exceptionReport.getSubscription()).isEqualTo("subscription");
    assertThat(exceptionReport.getExceptionClass()).isEqualTo("clazz");
    assertThat(exceptionReport.getExceptionMessage()).isEqualTo("message");
    assertThat(exceptionReport.getExceptionRootCause()).isEqualTo("root");
  }

  @Test
  void shouldPopulatePeekFields() {
    Peek peek = new Peek();
    peek.setMessageHash("hash");
    peek.setMessagePayload(new byte[] {1, 2, 3});

    assertThat(peek.getMessageHash()).isEqualTo("hash");
    assertThat(peek.getMessagePayload()).containsExactly(1, 2, 3);
  }

  @Test
  void shouldPopulateSkippedMessageFields() {
    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash("hash");
    skippedMessage.setMessagePayload(new byte[] {9, 8, 7});
    skippedMessage.setService("service");
    skippedMessage.setSubscription("subscription");
    skippedMessage.setRoutingKey("routing");
    skippedMessage.setContentType("application/json");
    skippedMessage.setHeaders(Map.of("k", "v"));

    assertThat(skippedMessage.getMessageHash()).isEqualTo("hash");
    assertThat(skippedMessage.getMessagePayload()).containsExactly(9, 8, 7);
    assertThat(skippedMessage.getService()).isEqualTo("service");
    assertThat(skippedMessage.getSubscription()).isEqualTo("subscription");
    assertThat(skippedMessage.getRoutingKey()).isEqualTo("routing");
    assertThat(skippedMessage.getContentType()).isEqualTo("application/json");
    assertThat(skippedMessage.getHeaders()).isEqualTo(Map.of("k", "v"));
  }

  @Test
  void shouldPopulateExceptionReportResponseFlags() {
    ExceptionReportResponse response = new ExceptionReportResponse();
    response.setPeek(true);
    response.setLogIt(false);
    response.setSkipIt(true);

    assertThat(response.isPeek()).isTrue();
    assertThat(response.isLogIt()).isFalse();
    assertThat(response.isSkipIt()).isTrue();
  }
}
