package com.example

import com.example.ui.MedicalWorkViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPresetCodeValidation() {
    // Valid medical test codes
    assertTrue(MedicalWorkViewModel.isValidPresetCode("AF07"))
    assertTrue(MedicalWorkViewModel.isValidPresetCode("MD-01"))
    assertTrue(MedicalWorkViewModel.isValidPresetCode("CBC"))
    assertTrue(MedicalWorkViewModel.isValidPresetCode("USG"))
    assertTrue(MedicalWorkViewModel.isValidPresetCode("101"))

    // Invalid - Patient IDs
    assertFalse(MedicalWorkViewModel.isValidPresetCode("AB260901"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("AB260948"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("PID12345"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("20260912"))

    // Invalid - Patient names
    assertFalse(MedicalWorkViewModel.isValidPresetCode("MD TUHIN"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("DR RAHIM"))

    // Invalid - JSON keywords and syntax
    assertFalse(MedicalWorkViewModel.isValidPresetCode("ID"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("patientId"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("name"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("date"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("{"))
    assertFalse(MedicalWorkViewModel.isValidPresetCode("}"))
  }

  @Test
  fun testExtractCodesOnlyFromJson() {
    val json = """
      [
        {"id": "AB260901", "code": "AF07", "name": "TUHIN"},
        {"id": "AB260902", "code": "MD-01", "name": "RAHIM"}
      ]
    """.trimIndent()
    val codes = MedicalWorkViewModel.extractCodesOnlyFromJson(json)
    assertEquals(listOf("AF07", "MD-01"), codes)
  }
}
