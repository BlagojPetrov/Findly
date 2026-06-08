package com.example.findly.ui.auth

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.findly.R
import com.example.findly.databinding.FragmentLoginBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.example.findly.MainActivity

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by activityViewModels()

    private lateinit var callbackManager: CallbackManager

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

            try {
                val account = task.getResult(
                    com.google.android.gms.common.api.ApiException::class.java
                )

                Log.d("GOOGLE", "account = $account")
                Log.d("GOOGLE", "idToken = ${account.idToken}")

                val token = account.idToken

                if (token != null) {
                    viewModel.signInWithGoogle(token)
                } else {
                    Log.e("GOOGLE", "ID TOKEN IS NULL")
                    Snackbar.make(
                        binding.root,
                        "Google sign-in failed: token null",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("GOOGLE", "Google sign-in error", e)

                Snackbar.make(
                    binding.root,
                    e.message ?: "Google sign in failed",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFacebookLogin()
        setupClickListeners()
        observeUiState()
    }

    private fun setupFacebookLogin() {
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {

                    val request = com.facebook.GraphRequest.newMeRequest(
                        result.accessToken
                    ) { obj, _ ->

                        val name = obj?.getString("name")

                        val pictureUrl = obj?.getJSONObject("picture")
                            ?.getJSONObject("data")
                            ?.getString("url")

                        viewModel.signInWithFacebook(
                            result.accessToken.token,
                            name,
                            pictureUrl
                        )
                    }

                    val parameters = Bundle()
                    parameters.putString("fields", "name,email,picture.type(large)")
                    request.parameters = parameters
                    request.executeAsync()
                }

                override fun onCancel() {
                    Snackbar.make(
                        binding.root,
                        getString(R.string.facebook_login_cancelled),
                        Snackbar.LENGTH_SHORT
                    ).show()
                }

                override fun onError(error: FacebookException) {
                    // Ignoriraj ja "Invalid Scopes" porakata — toa e samo warning
                    if (error.message?.contains("Invalid Scopes") == true) {
                        return
                    }
                    Snackbar.make(
                        binding.root,
                        error.message ?: getString(R.string.facebook_login_failed),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            viewModel.signInWithEmail(
                email = binding.etEmail.text?.toString() ?: "",
                password = binding.etPassword.text?.toString() ?: ""
            )
        }

        binding.btnGoogleSignIn.setOnClickListener {
            launchGoogleSignIn()
        }

        binding.btnFacebookSignIn.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                callbackManager,
                listOf("public_profile")
            )
        }

        binding.btnAnonymous.setOnClickListener {
            viewModel.signInAnonymously()
        }

        binding.btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot_password)
        }
    }

    @Suppress("DEPRECATION")
    private fun launchGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(requireActivity(), gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> showIdle()
                        is AuthUiState.Loading -> showLoading()
                        is AuthUiState.Success -> onAuthSuccess()
                        is AuthUiState.EmailNotVerified -> onEmailNotVerified()
                        is AuthUiState.VerificationEmailSent -> onVerificationEmailSent()
                        is AuthUiState.Error -> showError(state.message)
                    }
                }
            }
        }
    }

    private fun showIdle() {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.isEnabled = true
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignIn.isEnabled = false
    }

    private fun onAuthSuccess() {
        (requireActivity() as AuthActivity).goToMain()
        (requireActivity() as? MainActivity)?.getAnalytics()
            ?.logEvent(FirebaseAnalytics.Event.LOGIN) {
                param(FirebaseAnalytics.Param.METHOD, "email")
            }
    }

    private fun onEmailNotVerified() {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.isEnabled = true
        viewModel.resetState()
        findNavController().navigate(R.id.action_login_to_verification)
    }

    private fun onVerificationEmailSent() {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.isEnabled = true
        viewModel.resetState()
        findNavController().navigate(R.id.action_login_to_verification)
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnSignIn.isEnabled = true
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        viewModel.resetState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}