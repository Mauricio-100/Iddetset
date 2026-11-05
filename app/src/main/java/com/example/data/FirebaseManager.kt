package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _isEmailVerified = MutableStateFlow(auth.currentUser?.isEmailVerified ?: false)
    val isEmailVerified: StateFlow<Boolean> = _isEmailVerified

    fun checkEmailVerification() {
        auth.currentUser?.reload()?.addOnCompleteListener {
            _isEmailVerified.value = auth.currentUser?.isEmailVerified ?: false
        }
    }

    suspend fun sendVerificationEmail(): Boolean {
        return try {
            auth.currentUser?.sendEmailVerification()?.await()
            true
        } catch (e: Exception) {
            false
        }
    }
}
