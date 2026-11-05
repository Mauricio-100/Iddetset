package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    val isEmailVerified = FirebaseManager.isEmailVerified

    init {
        checkVerificationStatus()
    }

    fun checkVerificationStatus() {
        FirebaseManager.checkEmailVerification()
    }

    fun resendVerificationEmail(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = FirebaseManager.sendVerificationEmail()
            onResult(success)
        }
    }
}
