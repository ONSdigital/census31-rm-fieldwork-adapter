package uk.gov.ons.census.fieldworkadapter.utils;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class ObjectMapperFactory {

  private ObjectMapperFactory() {}

  public static ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .build();
  }
}
