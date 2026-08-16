package uk.gov.ons.census.fieldworkadapter.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.gov.ons.census.common.model.entity.SampleField;

class NewCaseTest {

  @Test
  void shouldReturnDefaultsForNullableAddressFields() {
    NewCase newCase = new NewCase();

    assertThat(newCase.getOrganisationName()).isEmpty();
    assertThat(newCase.getAddressLine2()).isEmpty();
    assertThat(newCase.getAddressLine3()).isEmpty();
  }

  @Test
  void shouldReturnMappedSampleFieldValues() {
    NewCase newCase = new NewCase();
    newCase.setCaseId(UUID.randomUUID());
    newCase.setCollectionExerciseId(UUID.randomUUID());
    newCase.setUprn("100000000000");
    newCase.setEstabUprn("200000000000");
    newCase.setAddressType("HH");
    newCase.setEstabType("CE");
    newCase.setAddressLevel("U");
    newCase.setAbpCode("A");
    newCase.setOrganisationName("Org");
    newCase.setAddressLine1("Line1");
    newCase.setAddressLine2("Line2");
    newCase.setAddressLine3("Line3");
    newCase.setTownName("Town");
    newCase.setPostcode("AB1 2CD");
    newCase.setLatitude("51.5");
    newCase.setLongitude("-1.2");
    newCase.setOa("OA");
    newCase.setLsoa("LSOA");
    newCase.setMsoa("MSOA");
    newCase.setLad("LAD");
    newCase.setRegion("E");
    newCase.setHtcWillingness("Y");
    newCase.setHtcDigital("N");
    newCase.setFieldCoordinatorId("coord");
    newCase.setFieldOfficerId("officer");
    newCase.setTreatmentCode("TR1");
    newCase.setCeExpectedCapacity(11);
    newCase.setPrintBatch("batch-1");
    newCase.setSecureEstablishment(true);

    assertThat(newCase.getSampleFieldValue(SampleField.UPRN)).isEqualTo("100000000000");
    assertThat(newCase.getSampleFieldValue(SampleField.ESTAB_UPRN)).isEqualTo("200000000000");
    assertThat(newCase.getSampleFieldValue(SampleField.ESTAB_TYPE)).isEqualTo("CE");
    assertThat(newCase.getSampleFieldValue(SampleField.ADDRESS_TYPE)).isEqualTo("HH");
    assertThat(newCase.getSampleFieldValue(SampleField.ABP_CODE)).isEqualTo("A");
    assertThat(newCase.getSampleFieldValue(SampleField.ORGANISATION_NAME)).isEqualTo("Org");
    assertThat(newCase.getSampleFieldValue(SampleField.ADDRESS_LINE1)).isEqualTo("Line1");
    assertThat(newCase.getSampleFieldValue(SampleField.ADDRESS_LINE2)).isEqualTo("Line2");
    assertThat(newCase.getSampleFieldValue(SampleField.ADDRESS_LINE3)).isEqualTo("Line3");
    assertThat(newCase.getSampleFieldValue(SampleField.ADDRESS_LEVEL)).isEqualTo("U");
    assertThat(newCase.getSampleFieldValue(SampleField.TOWN_NAME)).isEqualTo("Town");
    assertThat(newCase.getSampleFieldValue(SampleField.POSTCODE)).isEqualTo("AB1 2CD");
    assertThat(newCase.getSampleFieldValue(SampleField.LATITUDE)).isEqualTo("51.5");
    assertThat(newCase.getSampleFieldValue(SampleField.LONGITUDE)).isEqualTo("-1.2");
    assertThat(newCase.getSampleFieldValue(SampleField.OA)).isEqualTo("OA");
    assertThat(newCase.getSampleFieldValue(SampleField.LSOA)).isEqualTo("LSOA");
    assertThat(newCase.getSampleFieldValue(SampleField.MSOA)).isEqualTo("MSOA");
    assertThat(newCase.getSampleFieldValue(SampleField.LAD)).isEqualTo("LAD");
    assertThat(newCase.getSampleFieldValue(SampleField.REGION)).isEqualTo("E");
    assertThat(newCase.getSampleFieldValue(SampleField.HTC_WILLINGNESS)).isEqualTo("Y");
    assertThat(newCase.getSampleFieldValue(SampleField.HTC_DIGITAL)).isEqualTo("N");
    assertThat(newCase.getSampleFieldValue(SampleField.TREATMENT_CODE)).isEqualTo("TR1");
    assertThat(newCase.getSampleFieldValue(SampleField.FIELDCOORDINATOR_ID)).isEqualTo("coord");
    assertThat(newCase.getSampleFieldValue(SampleField.FIELDOFFICER_ID)).isEqualTo("officer");
    assertThat(newCase.getSampleFieldValue(SampleField.CE_EXPECTED_CAPACITY)).isEqualTo(11);
    assertThat(newCase.getSampleFieldValue(SampleField.PRINT_BATCH)).isEqualTo("batch-1");
    assertThat(newCase.getSampleFieldValue(SampleField.CE_SECURE)).isEqualTo(true);
  }
}
