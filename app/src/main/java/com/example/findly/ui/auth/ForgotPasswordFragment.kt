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
import com.example.findly.databinding.FragmentForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeForgotPasswordState()
    }

    private fun setupClickListeners() {
        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text?.toString() ?: ""
            viewModel.sendPasswordResetEmail(email)
        }

        binding.btnBackToLogin.setOnClickListener {
            findNavController().navigate(R.id.action_forgot_password_to_login)
        }
    }

    private fun observeForgotPasswordState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.forgotPasswordState.collect { state ->
                    when (state) {
                        is ForgotPasswordState.Idle -> showIdle()
                        is ForgotPasswordState.Loading -> showLoading()
                        is ForgotPasswordState.Success -> onSuccess()
                        is ForgotPasswordState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.btnSendReset.isEnabled = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSendReset.isEnabled = false
    }

    private fun onSuccess() {
        binding.progressBar.visibility = View.GONE
        binding.btnSendReset.isEnabled = true
        Snackbar.make(
            binding.root,
            getString(R.string.reset_email_sent),
            Snackbar.LENGTH_LONG
        ).show()
        viewModel.resetForgotPasswordState()
        findNavController().navigate(R.id.action_forgot_password_to_login)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSendReset.isEnabled = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetForgotPasswordState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}