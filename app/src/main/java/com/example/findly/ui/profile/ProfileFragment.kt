package com.example.findly.ui.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.findly.R
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.databinding.FragmentProfileBinding
import com.example.findly.ui.auth.AuthActivity
import com.example.findly.ui.home.ItemFeedAdapter
import com.example.findly.utils.ImagePickerHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private val authRepository = AuthRepositoryImpl()
    private lateinit var adapter: ItemFeedAdapter

    private var cameraImageUri: Uri? = null

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri -> viewModel.uploadProfilePhoto(uri) }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadProfilePhoto(it) }
    }

    // Camera permission
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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeUiState()
        observePhotoUpdateState()
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
        // Avatar tap → pick image
        binding.cardEditPhoto.setOnClickListener {
            showImagePickerDialog()
        }
        binding.ivAvatar.setOnClickListener {
            showImagePickerDialog()
        }

        // Sign out
        binding.btnSignOut.setOnClickListener {
            showSignOutDialog()
        }
    }

    private fun showImagePickerDialog() {
        val hasPhoto = (viewModel.uiState.value as? ProfileUiState.Success)?.photoUrl != null

        val options = if (hasPhoto) {
            arrayOf(
                getString(R.string.option_camera),
                getString(R.string.option_gallery),
                getString(R.string.option_remove_photo)
            )
        } else {
            arrayOf(
                getString(R.string.option_camera),
                getString(R.string.option_gallery)
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_pick_image_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndLaunch()
                    1 -> galleryLauncher.launch("image/*")
                    2 -> showRemovePhotoConfirmation()
                }
            }
            .show()
    }

    private fun showRemovePhotoConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_remove_photo_title))
            .setMessage(getString(R.string.dialog_remove_photo_message))
            .setPositiveButton(getString(R.string.btn_remove)) { _, _ ->
                viewModel.removeProfilePhoto()
            }
            .setNegativeButton(getString(R.string.cancel), null)
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

    private fun showSignOutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_sign_out_title))
            .setMessage(getString(R.string.dialog_sign_out_message))
            .setPositiveButton(getString(R.string.btn_sign_out)) { _, _ ->
                authRepository.signOut()
                goToAuth()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun goToAuth() {
        startActivity(Intent(requireContext(), AuthActivity::class.java))
        requireActivity().finish()
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

    private fun observePhotoUpdateState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.photoUpdateState.collect { state ->
                    when (state) {
                        is PhotoUpdateState.Idle    -> Unit
                        is PhotoUpdateState.Loading -> {
                            Snackbar.make(binding.root, getString(R.string.uploading_photo), Snackbar.LENGTH_SHORT).show()
                        }
                        is PhotoUpdateState.Success -> {
                            Snackbar.make(binding.root, getString(R.string.photo_updated), Snackbar.LENGTH_SHORT).show()
                            viewModel.resetPhotoState()
                        }
                        is PhotoUpdateState.Error   -> {
                            Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            viewModel.resetPhotoState()
                        }
                    }
                }
            }
        }
    }

    private fun showProfile(state: ProfileUiState.Success) {
        binding.tvDisplayName.text = state.displayName
        binding.tvEmail.text = state.email ?: getString(R.string.anonymous_user)
        binding.tvPostCount.text = state.myItems.size.toString()
        binding.tvSavedCount.text = state.savedCount.toString()

        // Load avatar
        if (state.photoUrl != null) {
            binding.ivAvatar.load(state.photoUrl) {
                crossfade(true)
                transformations(CircleCropTransformation())
                placeholder(R.drawable.ic_person)
                error(R.drawable.ic_person)
            }
        } else {
            binding.ivAvatar.setImageResource(R.drawable.ic_person)
        }

        binding.btnSignOut.text = if (state.isAnonymous) {
            getString(R.string.btn_sign_in)
        } else {
            getString(R.string.btn_sign_out)
        }

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