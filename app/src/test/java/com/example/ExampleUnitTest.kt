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

  @Test
  fun testUserExactJson() {
    val userJson = """
      {
        "date": "12/09/26",
        "columns": [
          "ক্রমিক",
          "ID",
          "কোড",
          "নাম"
        ],
        "data": [
          {
            "ক্রমিক": "১",
            "ID": "AB260954",
            "কোড": "AF07",
            "নাম": "SRE KISENO KUMAR"
          },
          {
            "ক্রমিক": "২",
            "ID": "AB260955",
            "কোড": "MD-01",
            "নাম": "SHIRABON MIA"
          },
          {
            "ক্রমিক": "৩",
            "ID": "AB260956",
            "কোড": "J007",
            "নাম": "MD SAIDUR RAHAMAN YOUSUF"
          },
          {
            "ক্রমিক": "৪",
            "ID": "AB260957",
            "কোড": "MD-01",
            "নাম": "NAZMUL HOSSANIN"
          },
          {
            "ক্রমিক": "৫",
            "ID": "AB260958",
            "কোড": "J007",
            "নাম": "MD SABOUS MIAH"
          },
          {
            "ক্রমিক": "৬",
            "ID": "AB260959",
            "কোড": "J007",
            "নাম": "MD AL AMININ"
          },
          {
            "ক্রমিক": "৭",
            "ID": "AB260960",
            "কোড": "J007",
            "নাম": "MD NAIM KHAN"
          },
          {
            "ক্রমিক": "৮",
            "ID": "AB260961",
            "কোড": "AF07",
            "নাম": "MD ALAMIN ALI"
          },
          {
            "ক্রমিক": "৯",
            "ID": "AB260962",
            "কোড": "J007",
            "নাম": "SOHEL SHEAK"
          }
        ]
      }
    """.trimIndent()

    val codes = MedicalWorkViewModel.extractCodesOnlyFromJson(userJson)
    println("Extracted codes: $codes")
    assertEquals(listOf("AF07", "MD-01", "J007"), codes)
  }
}
