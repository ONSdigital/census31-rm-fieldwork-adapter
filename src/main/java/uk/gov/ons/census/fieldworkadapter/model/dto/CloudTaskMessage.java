package uk.gov.ons.census.fieldworkadapter.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class CloudTaskMessage {
  private CloudTaskType cloudTaskType;
  private UUID correlationId;
  private Object payload;
}
