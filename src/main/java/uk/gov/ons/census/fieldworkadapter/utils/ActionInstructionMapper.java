package uk.gov.ons.census.fieldworkadapter.utils;

import org.springframework.stereotype.Component;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;

@Component
public class ActionInstructionMapper {

  private static final String SURVEY_NAME = "CENSUS";

  public FwmtActionInstructionDTO toFwmtActionInstruction(
      CaseUpdateDTO caseUpdate, FieldActionInstruction actionInstruction) {
    FwmtActionInstructionDTO mapped = new FwmtActionInstructionDTO();
    mapped.setActionInstruction(actionInstruction);
    mapped.setSurveyName(SURVEY_NAME);
    mapped.setCaseId(caseUpdate.getCaseId());
    mapped.setCaseRef(caseUpdate.getCaseRef());
    mapped.setOa(caseUpdate.getOa());
    mapped.setFieldCoordinatorId(caseUpdate.getFieldCoordinatorId());
    mapped.setFieldOfficerId(caseUpdate.getFieldOfficerId());
    mapped.setCeExpectedCapacity(caseUpdate.getCeExpectedCapacity());
    mapped.setSecureEstablishment(caseUpdate.isSecureEstablishment());
    mapped.setCe1Complete(isCeComplete(caseUpdate));

    if (caseUpdate.getAddress() != null) {
      mapped.setAddressType(caseUpdate.getAddress().getAddressType());
      mapped.setAddressLevel(caseUpdate.getAddress().getAddressLevel());
      mapped.setEstabType(caseUpdate.getAddress().getEstabType());
      mapped.setOrganisationName(caseUpdate.getAddress().getOrganisationName());
      mapped.setUprn(caseUpdate.getAddress().getUprn());
      mapped.setEstabUprn(caseUpdate.getAddress().getEstabUprn());
      mapped.setAddressLine1(caseUpdate.getAddress().getAddressLine1());
      mapped.setAddressLine2(caseUpdate.getAddress().getAddressLine2());
      mapped.setAddressLine3(caseUpdate.getAddress().getAddressLine3());
      mapped.setTownName(caseUpdate.getAddress().getTownName());
      mapped.setPostcode(caseUpdate.getAddress().getPostcode());
      mapped.setLatitude(parseCoordinate(caseUpdate.getAddress().getLatitude()));
      mapped.setLongitude(parseCoordinate(caseUpdate.getAddress().getLongitude()));
    }

    return mapped;
  }

  public FwmtCancelActionInstructionDTO toFwmtCancelActionInstruction(CaseUpdateDTO caseUpdate) {
    FwmtCancelActionInstructionDTO mapped = new FwmtCancelActionInstructionDTO();
    mapped.setActionInstruction(FieldActionInstruction.CANCEL);
    mapped.setSurveyName(SURVEY_NAME);
    mapped.setCaseId(caseUpdate.getCaseId());
    mapped.setCeExpectedCapacity(caseUpdate.getCeExpectedCapacity());

    if (caseUpdate.getAddress() != null) {
      mapped.setAddressType(caseUpdate.getAddress().getAddressType());
      mapped.setAddressLevel(caseUpdate.getAddress().getAddressLevel());
    }

    return mapped;
  }

  private boolean isCeComplete(CaseUpdateDTO caseUpdate) {
    return caseUpdate.getAddress() != null
        && "CE".equals(caseUpdate.getAddress().getAddressType())
        && "E".equals(caseUpdate.getAddress().getAddressLevel())
        && caseUpdate.isReceiptReceived();
  }

  private Double parseCoordinate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Double.parseDouble(value);
  }
}
