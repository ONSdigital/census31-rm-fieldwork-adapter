package uk.gov.ons.census.fieldworkadapter.utils;

import org.springframework.stereotype.Component;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtActionInstructionDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FwmtCancelActionInstructionDTO;

@Component
public class ActionInstructionMapper {

  private static final String SURVEY_NAME = "Census";
  private static final String CE_ADDRESS_TYPE = "CE";
  private static final String CE_UNIT_ADDRESS_LEVEL = "U";

  public FwmtActionInstructionDTO toFwmtActionInstruction(
      CaseUpdateDTO caseUpdate, FieldActionInstruction actionInstruction) {
    FwmtActionInstructionDTO mapped = new FwmtActionInstructionDTO();
    mapped.setActionInstruction(actionInstruction);
    mapped.setSurveyName(SURVEY_NAME);
    mapped.setCaseId(caseUpdate.getCaseId());

    if (caseUpdate.getAddress() != null) {
      mapped.setAddressType(caseUpdate.getAddress().getAddressType());

      if (isCeAddress(caseUpdate)) {
        mapped.setAddressLevel(caseUpdate.getAddress().getAddressLevel());
        if (actionInstruction == FieldActionInstruction.CREATE) {
          mapCeCreateFields(caseUpdate, mapped);
        } else if (actionInstruction == FieldActionInstruction.UPDATE) {
          mapCeUpdateFields(caseUpdate, mapped);
        }
      } else {
        mapHouseholdFields(caseUpdate, mapped);
      }
    }

    return mapped;
  }

  public FwmtCancelActionInstructionDTO toFwmtCancelActionInstruction(CaseUpdateDTO caseUpdate) {
    FwmtCancelActionInstructionDTO mapped = new FwmtCancelActionInstructionDTO();
    mapped.setActionInstruction(FieldActionInstruction.CANCEL);
    mapped.setSurveyName(SURVEY_NAME);
    mapped.setCaseId(caseUpdate.getCaseId());

    if (caseUpdate.getAddress() != null) {
      mapped.setAddressType(caseUpdate.getAddress().getAddressType());
      if (isCeAddress(caseUpdate)) {
        mapped.setAddressLevel(caseUpdate.getAddress().getAddressLevel());
      }
    }

    return mapped;
  }

  private void mapHouseholdFields(CaseUpdateDTO caseUpdate, FwmtActionInstructionDTO mapped) {
    mapped.setCaseRef(caseUpdate.getCaseRef());
    mapped.setOa(caseUpdate.getOa());
    mapped.setFieldOfficerId(caseUpdate.getFieldOfficerId());
    mapped.setUndeliveredAsAddress(caseUpdate.getUndeliveredAsAddress());
    mapped.setBlankFormReturned(caseUpdate.getBlankFormReturned());
    mapSharedAddressFields(caseUpdate, mapped);
  }

  private void mapCeCreateFields(CaseUpdateDTO caseUpdate, FwmtActionInstructionDTO mapped) {
    mapped.setCaseRef(caseUpdate.getCaseRef());
    mapped.setOa(caseUpdate.getOa());
    mapped.setFieldCoordinatorId(caseUpdate.getFieldCoordinatorId());
    mapped.setFieldOfficerId(caseUpdate.getFieldOfficerId());
    mapped.setCeExpectedCapacity(caseUpdate.getCeExpectedCapacity());
    mapped.setCeActualResponses(caseUpdate.getCeActualResponses());
    mapped.setSecureEstablishment(caseUpdate.isSecureEstablishment());
    mapped.setEstabType(caseUpdate.getAddress().getEstabType());
    mapped.setOrganisationName(caseUpdate.getAddress().getOrganisationName());
    mapped.setUprn(caseUpdate.getAddress().getUprn());
    if (isCeUnit(caseUpdate)) {
      mapped.setEstabUprn(caseUpdate.getAddress().getEstabUprn());
      mapped.setUndeliveredAsAddress(caseUpdate.getUndeliveredAsAddress());
    }
    mapSharedAddressFields(caseUpdate, mapped);
  }

  private void mapCeUpdateFields(CaseUpdateDTO caseUpdate, FwmtActionInstructionDTO mapped) {
    mapped.setCeExpectedCapacity(caseUpdate.getCeExpectedCapacity());
    mapped.setCeActualResponses(caseUpdate.getCeActualResponses());
  }

  private void mapSharedAddressFields(CaseUpdateDTO caseUpdate, FwmtActionInstructionDTO mapped) {
    mapped.setEstabType(caseUpdate.getAddress().getEstabType());
    mapped.setAddressLine1(caseUpdate.getAddress().getAddressLine1());
    mapped.setAddressLine2(caseUpdate.getAddress().getAddressLine2());
    mapped.setAddressLine3(caseUpdate.getAddress().getAddressLine3());
    mapped.setTownName(caseUpdate.getAddress().getTownName());
    mapped.setPostcode(caseUpdate.getAddress().getPostcode());
    mapped.setLatitude(parseCoordinate(caseUpdate.getAddress().getLatitude()));
    mapped.setLongitude(parseCoordinate(caseUpdate.getAddress().getLongitude()));
  }

  private boolean isCeAddress(CaseUpdateDTO caseUpdate) {
    return caseUpdate.getAddress() != null
        && CE_ADDRESS_TYPE.equals(caseUpdate.getAddress().getAddressType());
  }

  private boolean isCeUnit(CaseUpdateDTO caseUpdate) {
    return isCeAddress(caseUpdate)
        && CE_UNIT_ADDRESS_LEVEL.equals(caseUpdate.getAddress().getAddressLevel());
  }

  private Double parseCoordinate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Double.parseDouble(value);
  }
}
