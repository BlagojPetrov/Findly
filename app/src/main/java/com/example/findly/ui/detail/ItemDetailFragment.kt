package com.example.findly.ui.detail

import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
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
import androidx.navigation.fragment.navArgs
import com.example.findly.R
import com.example.findly.databinding.FragmentItemDetailBinding
import com.example.findly.domain.model.Item
import com.example.findly.domain.model.ItemType
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ItemDetailFragmentArgs by navArgs()

    private val viewModel: ItemDetailViewModel by viewModels {
        ItemDetailViewModel.Factory(requireActivity().application, args.itemId)
    }

    private lateinit var matchAdapter: MatchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupClickListeners()    // ← all clicks set here, once, immediately
        observeUiState()
        observeSavedState()
        observeOwnership()
        setupMatchesRecyclerView()
        observeMatches()
    }

    private fun observeOwnership() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isOwnItem.collect { isOwn ->
                    binding.btnDelete.visibility =
                        if (isOwn) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            viewModel.toggleSaved()
            // Show feedback based on current state (before toggle)
            val message = if (viewModel.isSaved.value) {
                getString(R.string.item_unsaved)
            } else {
                getString(R.string.item_saved)
            }
            Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
        }
        binding.btnContact.setOnClickListener {
            viewModel.getOrCreateConversation(
                onSuccess = { conversationId, otherUserDisplayName, itemTitle, otherUserPhotoUrl ->
                    val action = ItemDetailFragmentDirections
                        .actionDetailToChat(
                            conversationId = conversationId,
                            otherUserDisplayName = otherUserDisplayName,
                            itemTitle = itemTitle,
                            otherUserPhotoUrl = otherUserPhotoUrl
                        )
                    findNavController().navigate(action)
                },
                onError = { message ->
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                }
            )
        }
        binding.btnReport.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.report_coming_soon), Snackbar.LENGTH_SHORT).show()
        }
        binding.btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.dialog_delete_title))
                .setMessage(getString(R.string.dialog_delete_message))
                .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                    viewModel.deleteItem(
                        onSuccess = { findNavController().navigateUp() },
                        onError   = { message ->
                            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                        }
                    )
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DetailUiState.Loading -> showLoading()
                        is DetailUiState.Success -> showItem(state.item)
                        is DetailUiState.Error   -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun observeSavedState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isSaved.collect { isSaved ->
                    binding.btnSave.setIconResource(
                        if (isSaved) R.drawable.ic_bookmark
                        else R.drawable.ic_bookmark_outline
                    )
                }
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showItem(item: Item) {
        binding.progressBar.visibility = View.GONE

        binding.tvTitle.text = item.title
        binding.tvDescription.text = item.description
        binding.tvLocation.text = item.locationName
            ?: getString(R.string.location_unknown)
        binding.tvPostedBy.text = getString(R.string.posted_by, item.userDisplayName)
        binding.tvTimestamp.text = SimpleDateFormat(
            "MMM d, yyyy · HH:mm", Locale.getDefault()
        ).format(Date(item.timestamp))
        binding.tvCategory.text = item.category.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }

        binding.chipType.text = when (item.type) {
            ItemType.LOST  -> getString(R.string.type_lost)
            ItemType.FOUND -> getString(R.string.type_found)
        }
        binding.chipType.setChipBackgroundColorResource(
            when (item.type) {
                ItemType.LOST  -> R.color.chip_lost_background
                ItemType.FOUND -> R.color.chip_found_background
            }
        )
        binding.chipType.setTextColor(
            requireContext().getColor(
                when (item.type) {
                    ItemType.LOST  -> R.color.chip_lost_text
                    ItemType.FOUND -> R.color.chip_found_text
                }
            )
        )

        // Image
        if (item.imageUrl != null) {
            binding.ivItemImage.visibility = View.VISIBLE
            binding.ivItemImage.load(item.imageUrl) {
                crossfade(true)
            }
        } else {
            binding.ivItemImage.visibility = View.GONE
        }

        // Poster avatar
        if (item.userPhotoUrl != null) {
            binding.ivPosterAvatar.load(item.userPhotoUrl) {
                crossfade(true)
                transformations(coil.transform.CircleCropTransformation())
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
            }
        } else {
            binding.ivPosterAvatar.setImageResource(R.drawable.ic_person)
        }
    }


    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupMatchesRecyclerView() {
        matchAdapter = MatchAdapter { matchedItem ->
            val action = ItemDetailFragmentDirections
                .actionDetailToDetail(itemId = matchedItem.item.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewMatches.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = matchAdapter
        }
    }

    private fun observeMatches() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.matchesState.collect { state ->
                    when (state) {
                        is MatchesUiState.Loading -> {
                            binding.recyclerViewMatches.isVisible = false
                            binding.textViewMatchesTitle.isVisible = false
                        }
                        is MatchesUiState.Empty -> {
                            binding.recyclerViewMatches.isVisible = false
                            binding.textViewMatchesTitle.isVisible = false
                        }
                        is MatchesUiState.Success -> {
                            binding.textViewMatchesTitle.isVisible = true
                            binding.recyclerViewMatches.isVisible = true
                            matchAdapter.submitList(state.matches)
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