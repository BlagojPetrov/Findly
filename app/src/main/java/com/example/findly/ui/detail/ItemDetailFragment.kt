package com.example.findly.ui.detail

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

class ItemDetailFragment : Fragment() {

    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ItemDetailFragmentArgs by navArgs()

    private val viewModel: ItemDetailViewModel by viewModels {
        ItemDetailViewModel.Factory(requireActivity().application, args.itemId)
    }

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
        observeUiState()
        observeSavedState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
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

        binding.btnSave.setOnClickListener {
            viewModel.toggleSaved()
        }

        binding.btnContact.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.contact_coming_soon), Snackbar.LENGTH_SHORT).show()
        }

        binding.btnReport.setOnClickListener {
            Snackbar.make(binding.root, getString(R.string.report_coming_soon), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}