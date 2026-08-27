package uk.gov.ons.census.fieldworkadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class FwmtCancelActionInstructionDTO {
  private FieldActionInstruction actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private UUID caseId;
}
