package com.example.findly.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findly.databinding.FragmentConversationsBinding
import kotlinx.coroutines.launch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.findly.R

class ConversationsFragment : Fragment() {

    private var _binding: FragmentConversationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ConversationsViewModel by viewModels {
        ConversationsViewModelFactory()
    }

    private lateinit var adapter: ConversationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onClick = { conversation ->
                val action = ConversationsFragmentDirections
                    .actionConversationsToChat(
                        conversationId = conversation.id,
                        otherUserDisplayName = conversation.otherUserDisplayName,
                        otherUserPhotoUrl = conversation.otherUserPhotoUrl,
                        itemTitle = conversation.itemTitle
                    )
                findNavController().navigate(action)
            },
            onLongClick = { conversation ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_conversation)
                    .setMessage(R.string.delete_conversation_message)
                    .setPositiveButton(R.string.btn_delete) { _, _ ->
                        viewModel.deleteConversation(
                            conversationId = conversation.id,
                            onSuccess = {
                                Snackbar.make(
                                    binding.root,
                                    getString(R.string.conversation_deleted),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            },
                            onError = { message ->
                                Snackbar.make(
                                    binding.root,
                                    message,
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        )
        binding.recyclerViewConversations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ConversationsFragment.adapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ConversationsUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerViewConversations.isVisible = false
                            binding.textViewEmpty.isVisible = false
                        }
                        is ConversationsUiState.Empty -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewConversations.isVisible = false
                            binding.textViewEmpty.isVisible = true
                        }
                        is ConversationsUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewConversations.isVisible = true
                            binding.textViewEmpty.isVisible = false
                            adapter.submitList(state.conversations)
                        }
                        is ConversationsUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewConversations.isVisible = false
                            binding.textViewEmpty.isVisible = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}