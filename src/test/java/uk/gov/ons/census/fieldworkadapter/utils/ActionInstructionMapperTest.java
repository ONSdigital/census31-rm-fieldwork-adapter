package uk.gov.ons.census.fieldworkadapter.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;

class ActionInstructionMapperTest {

  private static final UUID CASE_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

  private final ActionInstructionMapper underTest = new ActionInstructionMapper();

  @Test
  void shouldMapHouseholdCreateActionInstructionToDocumentedPayload() {
    CaseUpdateDTO caseUpdate = buildHouseholdCaseUpdate("E92000001");

    var mapped = underTest.toFwmtActionInstruction(caseUpdate, FieldActionInstruction.CREATE);
    String json = JsonHelper.convertObjectToJson(mapped);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(mapped.getSurveyName()).isEqualTo("Census");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getCaseRef()).isEqualTo(caseUpdate.getCaseRef());
    assertThat(mapped.getAddressType()).isEqualTo("HH");
    assertThat(mapped.getAddressLevel()).isNull();
    assertThat(mapped.getFieldOfficerId()).isEqualTo(caseUpdate.getFieldOfficerId());
    assertThat(mapped.getFieldCoordinatorId()).isNull();
    assertThat(mapped.getBlankFormReturned()).isTrue();
    assertThat(mapped.getUndeliveredAsAddress()).isFalse();
    assertThat(mapped.getLatitude()).isEqualTo(51.5);
    assertThat(mapped.getLongitude()).isEqualTo(-1.2);
    assertThat(mapped.getCeExpectedCapacity()).isNull();
    assertThat(mapped.getCeActualResponses()).isNull();
    assertThat(mapped.getSecureEstablishment()).isNull();
    assertThat(json)
        .contains("\"blankFormReturned\":true", "\"undeliveredAsAddress\":false")
        .doesNotContain(
            "addressLevel", "ceExpectedCapacity", "ceActualResponses", "secureEstablishment");
  }

  @Test
  void shouldMapCeCreateUnitActionInstructionToDocumentedPayload() {
    CaseUpdateDTO caseUpdate = buildCeCaseUpdate("U", "E92000001");

    var mapped = underTest.toFwmtActionInstruction(caseUpdate, FieldActionInstruction.CREATE);
    String json = JsonHelper.convertObjectToJson(mapped);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(mapped.getSurveyName()).isEqualTo("Census");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getAddressType()).isEqualTo("CE");
    assertThat(mapped.getAddressLevel()).isEqualTo("U");
    assertThat(mapped.getUprn()).isEqualTo("10000000000");
    assertThat(mapped.getEstabUprn()).isEqualTo("20000000000");
    assertThat(mapped.getFieldOfficerId()).isEqualTo("FO12345");
    assertThat(mapped.getFieldCoordinatorId()).isEqualTo("FC001");
    assertThat(mapped.getOrganisationName()).isEqualTo("Example Organisation Ltd");
    assertThat(mapped.getCeExpectedCapacity()).isEqualTo(42);
    assertThat(mapped.getCeActualResponses()).isEqualTo(11);
    assertThat(mapped.getSecureEstablishment()).isFalse();
    assertThat(mapped.getUndeliveredAsAddress()).isTrue();
    assertThat(mapped.getBlankFormReturned()).isNull();
    assertThat(json)
        .contains(
            "\"addressLevel\":\"U\"",
            "\"secureEstablishment\":false",
            "\"ceExpectedCapacity\":42",
            "\"ceActualResponses\":11",
            "\"estabUprn\":\"20000000000\"",
            "\"undeliveredAsAddress\":true")
        .doesNotContain("blankFormReturned");
  }

  @Test
  void shouldMapCeUpdateActionInstructionToDocumentedMinimalPayload() {
    CaseUpdateDTO caseUpdate = buildCeCaseUpdate("E", "E92000001");

    var mapped = underTest.toFwmtActionInstruction(caseUpdate, FieldActionInstruction.UPDATE);
    String json = JsonHelper.convertObjectToJson(mapped);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.UPDATE);
    assertThat(mapped.getSurveyName()).isEqualTo("Census");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getAddressType()).isEqualTo("CE");
    assertThat(mapped.getAddressLevel()).isEqualTo("E");
    assertThat(mapped.getCeExpectedCapacity()).isEqualTo(42);
    assertThat(mapped.getCeActualResponses()).isEqualTo(11);
    assertThat(mapped.getCaseRef()).isNull();
    assertThat(mapped.getFieldOfficerId()).isNull();
    assertThat(mapped.getAddressLine1()).isNull();
    assertThat(mapped.getSecureEstablishment()).isNull();
    assertThat(json)
        .contains("\"ceExpectedCapacity\":42", "\"ceActualResponses\":11")
        .doesNotContain(
            "caseRef",
            "fieldOfficerId",
            "fieldCoordinatorId",
            "addressLine1",
            "organisationName",
            "uprn",
            "secureEstablishment");
  }

  @Test
  void shouldMapCancelActionInstructionWithOnlyDocumentedHouseholdFields() {
    CaseUpdateDTO caseUpdate = buildHouseholdCaseUpdate("E92000001");

    var mapped = underTest.toFwmtCancelActionInstruction(caseUpdate);
    String json = JsonHelper.convertObjectToJson(mapped);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(mapped.getSurveyName()).isEqualTo("Census");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getAddressType()).isEqualTo("HH");
    assertThat(mapped.getAddressLevel()).isNull();
    assertThat(json).doesNotContain("addressLevel", "ceExpectedCapacity", "ceActualResponses");
  }

  @Test
  void shouldMapCancelActionInstructionWithDocumentedCeFields() {
    CaseUpdateDTO caseUpdate = buildCeCaseUpdate("U", "E92000001");

    var mapped = underTest.toFwmtCancelActionInstruction(caseUpdate);
    String json = JsonHelper.convertObjectToJson(mapped);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(mapped.getSurveyName()).isEqualTo("Census");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getAddressType()).isEqualTo("CE");
    assertThat(mapped.getAddressLevel()).isEqualTo("U");
    assertThat(json)
        .contains("\"addressLevel\":\"U\"")
        .doesNotContain("ceExpectedCapacity", "ceActualResponses");
  }

  private CaseUpdateDTO buildHouseholdCaseUpdate(String region) {
    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(CASE_ID);
    caseUpdate.setCaseRef("12345678901");
    caseUpdate.setOa("E00000001");
    caseUpdate.setFieldCoordinatorId("FC001");
    caseUpdate.setFieldOfficerId("FO12345");
    caseUpdate.setCeExpectedCapacity(42);
    caseUpdate.setCeActualResponses(11);
    caseUpdate.setBlankFormReturned(true);
    caseUpdate.setUndeliveredAsAddress(false);
    caseUpdate.setSecureEstablishment(false);

    Address address = new Address();
    address.setRegion(region);
    address.setAddressType("HH");
    address.setAddressLevel("U");
    address.setEstabType("Residential Property");
    address.setOrganisationName("Example Organisation Ltd");
    address.setAddressLine1("1 High Street");
    address.setAddressLine2("Business Park");
    address.setAddressLine3("Unit 5");
    address.setTownName("Newport");
    address.setLatitude("51.5");
    address.setLongitude("-1.2");
    address.setPostcode("AB1 2CD");
    address.setUprn("10000000000");
    address.setEstabUprn("20000000000");

    caseUpdate.setAddress(address);
    return caseUpdate;
  }

  private CaseUpdateDTO buildCeCaseUpdate(String addressLevel, String region) {
    CaseUpdateDTO caseUpdate = buildHouseholdCaseUpdate(region);
    caseUpdate.getAddress().setAddressType("CE");
    caseUpdate.getAddress().setAddressLevel(addressLevel);
    caseUpdate.setUndeliveredAsAddress(true);
    caseUpdate.setBlankFormReturned(null);
    return caseUpdate;
  }
}
