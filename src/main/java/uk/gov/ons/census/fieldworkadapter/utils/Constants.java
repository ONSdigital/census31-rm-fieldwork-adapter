package uk.gov.ons.census.fieldworkadapter.utils;

import java.util.Set;

public class Constants {
  public static final String OUTBOUND_EVENT_SCHEMA_VERSION = "1.0.0";
  public static final Set<String> ALLOWED_INBOUND_EVENT_SCHEMA_VERSIONS = Set.of("1.0.0");

  public static final String REQUEST_PERSONALISATION_PREFIX = "__request__.";
}
