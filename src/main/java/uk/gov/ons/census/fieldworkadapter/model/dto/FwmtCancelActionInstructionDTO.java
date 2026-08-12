package uk.gov.ons.census.fieldworkadapter.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class FwmtCancelActionInstructionDTO {
  private FieldActionInstruction actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private UUID caseId;
  private Integer ceExpectedCapacity;
  private int ceActualResponses;
}
