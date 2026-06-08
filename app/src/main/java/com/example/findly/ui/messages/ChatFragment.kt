package com.example.findly.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findly.databinding.FragmentChatBinding
import kotlinx.coroutines.launch
import com.google.firebase.analytics.logEvent
import com.example.findly.MainActivity

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory()
    }

    private val args: ChatFragmentArgs by navArgs()
    private lateinit var adapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupInput()
        observeUiState()
        observeSendState()
        viewModel.setOtherUserPhotoUrl(args.otherUserPhotoUrl)
        viewModel.loadMessages(args.conversationId)
        viewModel.markAsRead(args.conversationId)
    }

    override fun onResume() {
        super.onResume()
        viewModel.markAsRead(args.conversationId)
    }

    override fun onPause() {
        super.onPause()
        viewModel.markAsRead(args.conversationId)
    }

    private fun setupToolbar() {
        binding.textViewToolbarTitle.text = args.otherUserDisplayName
        binding.textViewToolbarSubtitle.text = args.itemTitle
        binding.buttonBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            currentUserId = viewModel.currentUserId,
            otherUserPhotoUrl = args.otherUserPhotoUrl
        )
        binding.recyclerViewMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = this@ChatFragment.adapter
        }
    }

    private fun setupInput() {
        binding.editTextMessage.doAfterTextChanged { text ->
            binding.buttonSend.isEnabled = !text.isNullOrBlank()
        }
        binding.buttonSend.setOnClickListener {
            val text = binding.editTextMessage.text?.toString() ?: return@setOnClickListener
            viewModel.sendMessage(args.conversationId, text)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ChatUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.recyclerViewMessages.isVisible = false
                        }
                        is ChatUiState.Empty -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewMessages.isVisible = false
                            binding.textViewEmpty.isVisible = true
                        }
                        is ChatUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.recyclerViewMessages.isVisible = true
                            binding.textViewEmpty.isVisible = false
                            adapter.submitList(state.messages) {
                                binding.recyclerViewMessages.scrollToPosition(
                                    adapter.itemCount - 1
                                )
                            }
                        }
                        is ChatUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.textViewEmpty.isVisible = true
                        }
                    }
                }
            }
        }
    }

    private fun observeSendState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sendState.collect { state ->
                    when (state) {
                        is SendMessageState.Idle -> {
                            binding.buttonSend.isEnabled =
                                !binding.editTextMessage.text.isNullOrBlank()
                        }
                        is SendMessageState.Sending -> {
                            binding.buttonSend.isEnabled = false
                        }
                        is SendMessageState.Sent -> {
                            binding.editTextMessage.setText("")
                            viewModel.resetSendState()
                        }
                        is SendMessageState.Error -> {
                            binding.buttonSend.isEnabled = true
                            viewModel.resetSendState()
                        }
                        is SendMessageState.Sent -> {
                            binding.editTextMessage.setText("")
                            (requireActivity() as? MainActivity)?.getAnalytics()
                                ?.logEvent("message_sent") {
                                    param("conversation_id", args.conversationId)
                                }
                            viewModel.resetSendState()
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