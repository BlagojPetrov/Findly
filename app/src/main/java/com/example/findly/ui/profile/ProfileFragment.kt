package com.example.findly.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.findly.R
import com.example.findly.databinding.FragmentProfileBinding
import com.example.findly.ui.home.ItemFeedAdapter
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var adapter: ItemFeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = ItemFeedAdapter(
            onItemClick = { item ->
                val action = ProfileFragmentDirections.actionProfileToDetail(itemId = item.id)
                findNavController().navigate(action)
            }
        )
        binding.recyclerMyPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProfileFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnSignOut.setOnClickListener {
            // Wired to real auth after Firebase integration
            Snackbar.make(binding.root, getString(R.string.auth_coming_soon), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ProfileUiState.Loading -> Unit
                        is ProfileUiState.Success -> showProfile(state)
                    }
                }
            }
        }
    }

    private fun showProfile(state: ProfileUiState.Success) {
        binding.tvDisplayName.text = state.displayName
        binding.tvEmail.text = state.email ?: getString(R.string.not_signed_in)
        binding.tvPostCount.text = state.myItems.size.toString()
        binding.tvSavedCount.text = state.savedCount.toString()

        if (state.myItems.isEmpty()) {
            binding.recyclerMyPosts.visibility = View.GONE
            binding.emptyPosts.visibility = View.VISIBLE
        } else {
            binding.recyclerMyPosts.visibility = View.VISIBLE
            binding.emptyPosts.visibility = View.GONE
            adapter.submitList(state.myItems)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}