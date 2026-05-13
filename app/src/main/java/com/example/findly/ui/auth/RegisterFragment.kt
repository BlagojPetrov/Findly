package com.example.findly.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.findly.R
import com.example.findly.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeUiState()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            viewModel.registerWithEmail(
                email = binding.etEmail.text?.toString() ?: "",
                password = binding.etPassword.text?.toString() ?: "",
                confirmPassword = binding.etConfirmPassword.text?.toString() ?: "",
                displayName = binding.etDisplayName.text?.toString() ?: ""
            )
        }

        binding.btnGoToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> showIdle()
                        is AuthUiState.Loading -> showLoading()
                        is AuthUiState.Success -> onAuthSuccess()
                        is AuthUiState.VerificationEmailSent -> onVerificationEmailSent()
                        is AuthUiState.EmailNotVerified -> onEmailNotVerified()
                        is AuthUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false
    }

    private fun onAuthSuccess() {
        (requireActivity() as AuthActivity).goToMain()
    }

    private fun onVerificationEmailSent() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
        viewModel.resetState()
        findNavController().navigate(R.id.action_register_to_verification)
    }

    private fun onEmailNotVerified() {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
        viewModel.resetState()
        findNavController().navigate(R.id.action_register_to_verification)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnRegister.isEnabled = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}