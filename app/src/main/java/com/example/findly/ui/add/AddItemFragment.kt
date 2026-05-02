package com.example.findly.ui.add

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.findly.R
import com.example.findly.databinding.FragmentAddItemBinding
import com.example.findly.domain.model.Category
import com.example.findly.domain.model.ItemType
import com.example.findly.utils.ImagePickerHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddItemViewModel by viewModels()
    private var selectedType: ItemType = ItemType.LOST
    private var cameraImageUri: Uri? = null

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                viewModel.onImageSelected(uri)
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Snackbar.make(binding.root, getString(R.string.camera_permission_denied), Snackbar.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupTypeToggle()
        setupCategoryDropdown()
        setupImagePicker()
        setupSubmitButton()
        observeUiState()
        observeSelectedImage()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupTypeToggle() {
        binding.toggleItemType.check(R.id.btnTypeLost)
        binding.toggleItemType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedType = when (checkedId) {
                    R.id.btnTypeLost  -> ItemType.LOST
                    R.id.btnTypeFound -> ItemType.FOUND
                    else              -> ItemType.LOST
                }
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categories = Category.entries.map { category ->
            category.name.lowercase().replaceFirstChar { it.uppercase() }
        }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        binding.actvCategory.setAdapter(adapter)
        binding.actvCategory.setText(categories.first(), false)
    }

    private fun setupImagePicker() {
        binding.cardImage.setOnClickListener {
            showImagePickerDialog()
        }
        binding.btnRemoveImage.setOnClickListener {
            viewModel.clearSelectedImage()
        }
    }

    private fun showImagePickerDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_pick_image_title))
            .setItems(
                arrayOf(
                    getString(R.string.option_camera),
                    getString(R.string.option_gallery)
                )
            ) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        cameraImageUri = ImagePickerHelper.createImageUri(requireContext())
        cameraLauncher.launch(cameraImageUri)
    }

    private fun observeSelectedImage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedImageUri.collect { uri ->
                    if (uri != null) {
                        binding.ivImagePreview.visibility = View.VISIBLE
                        binding.btnRemoveImage.visibility = View.VISIBLE
                        binding.layoutImagePlaceholder.visibility = View.GONE
                        binding.ivImagePreview.load(uri) {
                            crossfade(true)
                        }
                    } else {
                        binding.ivImagePreview.visibility = View.GONE
                        binding.btnRemoveImage.visibility = View.GONE
                        binding.layoutImagePlaceholder.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmit.setOnClickListener {
            val selectedCategoryName = binding.actvCategory.text.toString()
                .uppercase().replace(" ", "_")
            val category = try {
                Category.valueOf(selectedCategoryName)
            } catch (e: IllegalArgumentException) {
                Category.OTHER
            }
            viewModel.submitItem(
                type         = selectedType,
                title        = binding.etTitle.text?.toString() ?: "",
                description  = binding.etDescription.text?.toString() ?: "",
                category     = category,
                locationName = binding.etLocation.text?.toString() ?: ""
            )
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AddItemUiState.Idle    -> showIdle()
                        is AddItemUiState.Loading -> showLoading()
                        is AddItemUiState.Success -> onSuccess()
                        is AddItemUiState.Error   -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.btnSubmit.isEnabled = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSubmit.isEnabled = false
    }

    private fun onSuccess() {
        binding.progressBar.visibility = View.GONE
        findNavController().navigateUp()
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSubmit.isEnabled = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}