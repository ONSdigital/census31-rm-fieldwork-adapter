package uk.gov.ons.census.fieldworkadapter.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.FieldActionInstruction;

class ActionInstructionMapperTest {

  private final ActionInstructionMapper underTest = new ActionInstructionMapper();

  @Test
  void shouldMapCreateActionInstruction() {
    CaseUpdateDTO caseUpdate = buildCaseUpdate("E");

    var mapped = underTest.toFwmtActionInstruction(caseUpdate, FieldActionInstruction.CREATE);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CREATE);
    assertThat(mapped.getSurveyName()).isEqualTo("CENSUS");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getCaseRef()).isEqualTo(caseUpdate.getCaseRef());
    assertThat(mapped.getAddressType()).isEqualTo("HH");
    assertThat(mapped.getAddressLevel()).isEqualTo("U");
    assertThat(mapped.getLatitude()).isEqualTo(51.5);
    assertThat(mapped.getLongitude()).isEqualTo(-1.2);
  }

  @Test
  void shouldMapCancelActionInstructionWithMinimalFields() {
    CaseUpdateDTO caseUpdate = buildCaseUpdate("E");

    var mapped = underTest.toFwmtCancelActionInstruction(caseUpdate);

    assertThat(mapped.getActionInstruction()).isEqualTo(FieldActionInstruction.CANCEL);
    assertThat(mapped.getSurveyName()).isEqualTo("CENSUS");
    assertThat(mapped.getCaseId()).isEqualTo(caseUpdate.getCaseId());
    assertThat(mapped.getAddressType()).isEqualTo("HH");
    assertThat(mapped.getAddressLevel()).isEqualTo("U");
    assertThat(mapped.getCeExpectedCapacity()).isEqualTo(caseUpdate.getCeExpectedCapacity());
  }

  @Test
  void shouldSetCeCompleteForCeEstablishmentWithReceipt() {
    CaseUpdateDTO caseUpdate = buildCaseUpdate("E");
    caseUpdate.getAddress().setAddressType("CE");
    caseUpdate.getAddress().setAddressLevel("E");
    caseUpdate.setReceiptReceived(true);

    var mapped = underTest.toFwmtActionInstruction(caseUpdate, FieldActionInstruction.UPDATE);

    assertThat(mapped.isCe1Complete()).isTrue();
  }

  private CaseUpdateDTO buildCaseUpdate(String region) {
    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseId(UUID.randomUUID());
    caseUpdate.setCaseRef("1234567890");
    caseUpdate.setOa("E00000001");
    caseUpdate.setFieldCoordinatorId("1");
    caseUpdate.setFieldOfficerId("2");
    caseUpdate.setCeExpectedCapacity(12);
    caseUpdate.setSecureEstablishment(false);

    Address address = new Address();
    address.setRegion(region);
    address.setAddressType("HH");
    address.setAddressLevel("U");
    address.setLatitude("51.5");
    address.setLongitude("-1.2");
    address.setPostcode("AB1 2CD");
    address.setUprn("10000000000");
    address.setEstabUprn("20000000000");

    caseUpdate.setAddress(address);
    return caseUpdate;
  }
}
