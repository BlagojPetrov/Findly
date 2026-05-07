package com.example.findly.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.findly.data.remote.firestore.FirestoreMessageSource
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.data.repository.MessageRepositoryImpl

class ConversationsViewModelFactory : ViewModelProvider.Factory {

    private val firestoreMessageSource = FirestoreMessageSource()
    private val messageRepository = MessageRepositoryImpl(firestoreMessageSource)
    private val authRepository = AuthRepositoryImpl()

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationsViewModel::class.java)) {
            return ConversationsViewModel(
                messageRepository = messageRepository,
                authRepository = authRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}