package com.example

import com.example.ui.components.VerificationState
import com.example.ui.components.getVerificationState
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVerificationStateLogic() {
    // Official founder accounts
    assertEquals(VerificationState.OFFICIAL, getVerificationState("C.M.O", isVerified = false))
    assertEquals(VerificationState.OFFICIAL, getVerificationState("Doffranel", isVerified = true))
    assertEquals(VerificationState.OFFICIAL, getVerificationState("Crislem", isVerified = false))
    assertEquals(VerificationState.OFFICIAL, getVerificationState("Mauricio-100", isVerified = true))

    // Non-founder but verified accounts
    assertEquals(VerificationState.VERIFIED, getVerificationState("RegularUser", isVerified = true))
    assertEquals(VerificationState.VERIFIED, getVerificationState("JohnDoe", isVerified = true))

    // Non-founder and unverified accounts
    assertEquals(VerificationState.NONE, getVerificationState("RegularUser", isVerified = false))
    assertEquals(VerificationState.NONE, getVerificationState(null, isVerified = false))
  }
}
