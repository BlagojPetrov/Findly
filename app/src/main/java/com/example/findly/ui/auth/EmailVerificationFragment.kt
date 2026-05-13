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
import com.example.findly.databinding.FragmentEmailVerificationBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class EmailVerificationFragment : Fragment() {

    private var _binding: FragmentEmailVerificationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmailVerificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeVerificationState()
    }

    private fun setupClickListeners() {
        binding.btnCheckVerification.setOnClickListener {
            viewModel.checkEmailVerification()
        }

        binding.btnResendVerification.setOnClickListener {
            viewModel.resendVerificationEmail()
        }

        binding.btnBackToLogin.setOnClickListener {
            viewModel.resetVerificationState()
            findNavController().navigate(R.id.action_verification_to_login)
        }
    }

    private fun observeVerificationState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.verificationState.collect { state ->
                    when (state) {
                        is VerificationState.Idle -> showIdle()
                        is VerificationState.Loading -> showLoading()
                        is VerificationState.Verified -> onVerified()
                        is VerificationState.NotVerified -> onNotVerified()
                        is VerificationState.EmailResent -> onEmailResent()
                        is VerificationState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.btnCheckVerification.isEnabled = true
        binding.btnResendVerification.isEnabled = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCheckVerification.isEnabled = false
        binding.btnResendVerification.isEnabled = false
    }

    private fun onVerified() {
        binding.progressBar.visibility = View.GONE
        Snackbar.make(
            binding.root,
            getString(R.string.email_verified_success),
            Snackbar.LENGTH_SHORT
        ).show()
        viewModel.resetVerificationState()
        (requireActivity() as AuthActivity).goToMain()
    }

    private fun onNotVerified() {
        binding.progressBar.visibility = View.GONE
        binding.btnCheckVerification.isEnabled = true
        binding.btnResendVerification.isEnabled = true
        Snackbar.make(
            binding.root,
            getString(R.string.email_not_verified_yet),
            Snackbar.LENGTH_LONG
        ).show()
        viewModel.resetVerificationState()
    }

    private fun onEmailResent() {
        binding.progressBar.visibility = View.GONE
        binding.btnCheckVerification.isEnabled = true
        binding.btnResendVerification.isEnabled = true
        Snackbar.make(
            binding.root,
            getString(R.string.verification_email_resent),
            Snackbar.LENGTH_LONG
        ).show()
        viewModel.resetVerificationState()
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnCheckVerification.isEnabled = true
        binding.btnResendVerification.isEnabled = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetVerificationState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}