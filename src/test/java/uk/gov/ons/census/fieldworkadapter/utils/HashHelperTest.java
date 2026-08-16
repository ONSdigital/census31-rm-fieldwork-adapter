package uk.gov.ons.census.fieldworkadapter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HashHelperTest {

  @Test
  void testStringToHash() {
    String testString = "This is a test for String to Hash!";
    String hashStringResult = HashHelper.hash(testString);
    assertEquals(
        "f45d7484129e46115bc11027723a92e853c708cbb204ae8e7d3381532281e264", hashStringResult);
  }

  @Test
  void testBytesToHash() {
    String testString = "This is a test for Bytes to Hash!";
    byte[] testBytes = testString.getBytes();

    String hashStringResult = HashHelper.hash(testBytes);
    assertEquals(
        "54443bae36f59b02652aac972646f83c6bc0e497da416a92198a83af62afe52d", hashStringResult);
  }
}
