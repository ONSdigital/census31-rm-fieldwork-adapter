package uk.gov.ons.census.fieldworkadapter.model.dto;

import java.util.UUID;
import lombok.Data;

@Data
public class FwmtActionInstructionDTO {
  private FieldActionInstruction actionInstruction;
  private String surveyName;
  private String addressType;
  private String addressLevel;
  private UUID caseId;
  private String caseRef;
  private String estabType;
  private String fieldOfficerId;
  private String fieldCoordinatorId;
  private String organisationName;
  private String uprn;
  private String estabUprn;
  private String addressLine1;
  private String addressLine2;
  private String addressLine3;
  private String townName;
  private String postcode;
  private String oa;
  private Double latitude;
  private Double longitude;
  private boolean ce1Complete;
  private boolean handDeliver;
  private Integer ceExpectedCapacity;
  private int ceActualResponses;
  private Boolean undeliveredAsAddress;
  private Boolean blankFormReturned;
  private boolean secureEstablishment;
}
