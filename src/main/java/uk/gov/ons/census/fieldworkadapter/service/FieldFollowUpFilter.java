package uk.gov.ons.census.fieldworkadapter.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.gov.ons.census.fieldworkadapter.model.dto.CaseUpdateDTO;

// CN-80: Field follow-up eligibility filter. Rules evaluated in order; first match returned.
// Asymmetries: Receipt excludes only HH (not CE/SPG). Unknown region fails open (stays eligible).
@Service
public class FieldFollowUpFilter {

  public enum Exclusion {
    NULL_CASE,
    INVALID,
    REFUSED,
    HH_ALREADY_RECEIPTED,
    HI_CASE_TYPE,
    EXCLUDED_REGION,
    ONLINE_ONLY_TREATMENT
  }

  private static final String CASE_TYPE_HH = "HH";
  private static final String CASE_TYPE_HI = "HI";
  private static final Set<String> EXCLUDED_REGION_PREFIXES = Set.of("N", "S");
  private static final Set<String> ONLINE_ONLY_TREATMENTS = Set.of("HH_ONE", "HH_ONW");

  public Optional<Exclusion> exclusionFor(CaseUpdateDTO caseUpdate) {
    if (caseUpdate == null) {
      return Optional.of(Exclusion.NULL_CASE);
    }

    String caseType = normalise(caseUpdate.getCaseType());

    if (caseUpdate.isInvalid()) {
      return Optional.of(Exclusion.INVALID);
    }
    if (caseUpdate.getRefusalReceived() != null) {
      return Optional.of(Exclusion.REFUSED);
    }
    if (CASE_TYPE_HH.equals(caseType) && caseUpdate.isReceiptReceived()) {
      return Optional.of(Exclusion.HH_ALREADY_RECEIPTED);
    }
    if (CASE_TYPE_HI.equals(caseType)) {
      return Optional.of(Exclusion.HI_CASE_TYPE);
    }
    if (isExcludedRegion(caseUpdate)) {
      return Optional.of(Exclusion.EXCLUDED_REGION);
    }
    if (ONLINE_ONLY_TREATMENTS.contains(normalise(caseUpdate.getTreatmentCode()))) {
      return Optional.of(Exclusion.ONLINE_ONLY_TREATMENT);
    }
    return Optional.empty();
  }

  public boolean isValidForFieldFollowUp(CaseUpdateDTO caseUpdate) {
    return exclusionFor(caseUpdate).isEmpty();
  }

  private boolean isExcludedRegion(CaseUpdateDTO caseUpdate) {
    if (caseUpdate.getAddress() == null || caseUpdate.getAddress().getRegion() == null) {
      return false; // fail-open: unknown region stays eligible
    }

    String region = normalise(caseUpdate.getAddress().getRegion());
    if (region == null) {
      return false; // fail-open: whitespace-only region stays eligible
    }

    return EXCLUDED_REGION_PREFIXES.stream().anyMatch(region::startsWith);
  }

  private static String normalise(String value) {
    return value == null ? "" : value.strip().toUpperCase(Locale.ROOT);
  }
}
