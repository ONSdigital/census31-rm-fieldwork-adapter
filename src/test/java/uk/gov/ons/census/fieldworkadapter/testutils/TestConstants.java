package uk.gov.ons.census.fieldworkadapter.testutils;

import java.util.Map;
import java.util.UUID;

public class TestConstants {
  public static final String OUR_PUBSUB_PROJECT = "our-project";
  public static final String OUTBOUND_CASE_SUBSCRIPTION = "event_case-update_rh";

  public static final UUID TEST_CORRELATION_ID = UUID.randomUUID();
  public static final String TEST_ORIGINATING_USER = "foo@bar.com";
  public static final Map<String, String> TEST_UAC_METADATA = Map.of("TEST_UAC_METADATA", "TEST");
}
