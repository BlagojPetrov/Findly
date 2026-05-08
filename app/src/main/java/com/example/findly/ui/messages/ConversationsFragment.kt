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
        adapter = ConversationAdapter { conversation ->
            val action = ConversationsFragmentDirections
                .actionConversationsToChat(
                    conversationId = conversation.id,
                    otherUserDisplayName = conversation.otherUserDisplayName,
                    otherUserPhotoUrl = conversation.otherUserPhotoUrl,
                    itemTitle = conversation.itemTitle
                )
            findNavController().navigate(action)
        }
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
                            binding.textViewEmpty.text = state.message
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