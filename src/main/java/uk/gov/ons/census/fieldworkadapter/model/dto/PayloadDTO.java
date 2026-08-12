package uk.gov.ons.census.fieldworkadapter.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;

@Data
@JsonInclude(Include.NON_NULL)
public class PayloadDTO {
  private CaseUpdateDTO caseUpdate;
  private NewCase newCase;
}
