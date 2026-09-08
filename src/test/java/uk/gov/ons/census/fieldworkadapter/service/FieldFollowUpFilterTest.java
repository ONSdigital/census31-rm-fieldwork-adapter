package uk.gov.ons.census.fieldworkadapter.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import uk.gov.ons.census.fieldworkadapter.model.dto.Address;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;
import uk.gov.ons.census.fieldworkadapter.model.dto.RefusalTypeDTO;
import uk.gov.ons.census.fieldworkadapter.service.FieldFollowUpFilter.Exclusion;

class FieldFollowUpFilterTest {

  private final FieldFollowUpFilter underTest = new FieldFollowUpFilter();

  @Test
  void shouldExcludeNullCase() {
    assertThat(underTest.exclusionFor(null)).contains(Exclusion.NULL_CASE);
  }

  @Test
  void shouldExcludeInvalidCase() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setInvalid(true);

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.INVALID);
  }

  @Test
  void shouldExcludeRefusedCase() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setRefusalReceived(RefusalTypeDTO.HARD_REFUSAL);

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.REFUSED);
  }

  @Test
  void shouldExcludeReceiptedHouseholdCase() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setReceiptReceived(true);

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.HH_ALREADY_RECEIPTED);
  }

  @Test
  void shouldExcludeHiCaseType() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setCaseType("HI");

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.HI_CASE_TYPE);
  }

  @Test
  void shouldExcludeScottishAndNorthernIrishRegions() {
    CaseUpdateDTO northernIrishCase = eligibleCase();
    northernIrishCase.getAddress().setRegion("N92000002");
    CaseUpdateDTO scottishCase = eligibleCase();
    scottishCase.getAddress().setRegion(" s92000003 ");

    assertThat(underTest.exclusionFor(northernIrishCase)).contains(Exclusion.EXCLUDED_REGION);
    assertThat(underTest.exclusionFor(scottishCase)).contains(Exclusion.EXCLUDED_REGION);
  }

  @Test
  void shouldExcludeOnlineOnlyTreatment() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setTreatmentCode("HH_ONE");

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.ONLINE_ONLY_TREATMENT);
  }

  @Test
  void shouldReturnFirstMatchingExclusion() {
    CaseUpdateDTO caseUpdate = eligibleCase();
    caseUpdate.setInvalid(true);
    caseUpdate.setRefusalReceived(RefusalTypeDTO.SOFT_REFUSAL);
    caseUpdate.setReceiptReceived(true);
    caseUpdate.setCaseType("HI");
    caseUpdate.setTreatmentCode("HH_ONE");
    caseUpdate.getAddress().setRegion("S92000003");

    assertThat(underTest.exclusionFor(caseUpdate)).contains(Exclusion.INVALID);
  }

  @Test
  void shouldTreatEligibleCaseAsValidForFieldFollowUp() {
    assertThat(underTest.isValidForFieldFollowUp(eligibleCase())).isTrue();
  }

  private CaseUpdateDTO eligibleCase() {
    Address address = new Address();
    address.setRegion("E92000001");

    CaseUpdateDTO caseUpdate = new CaseUpdateDTO();
    caseUpdate.setCaseType("HH");
    caseUpdate.setAddress(address);
    return caseUpdate;
  }
}
