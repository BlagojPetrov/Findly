package com.example.findly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.findly.data.remote.firestore.FirestoreMessageSource
import com.example.findly.data.repository.AuthRepositoryImpl
import com.example.findly.data.repository.MessageRepositoryImpl
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: com.example.findly.databinding.ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var analytics: FirebaseAnalytics

    private val hiddenNavDestinations = setOf(
        R.id.itemDetailFragment,
        R.id.addItemFragment,
        R.id.chatFragment
    )

    private val authRepository by lazy { AuthRepositoryImpl() }
    private val messageRepository by lazy {
        MessageRepositoryImpl(FirestoreMessageSource())
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.example.findly.databinding.ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        analytics = FirebaseAnalytics.getInstance(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in hiddenNavDestinations) {
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.bottomNavigationView.visibility = View.VISIBLE
            }
            logScreenView(destination.label?.toString() ?: "Unknown")
        }

        requestNotificationPermission()
        observeUnreadCount()
    }

    private fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun observeUnreadCount() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                authRepository.authStateFlow.collect { user ->
                    if (user != null) {
                        messageRepository.observeConversations(user.uid)
                            .map { conversations -> conversations.sumOf { it.unreadCount } }
                            .catch { /* silently ignore badge errors */ }
                            .collect { totalUnread ->
                                updateMessagesBadge(totalUnread)
                            }
                    } else {
                        updateMessagesBadge(0)
                    }
                }
            }
        }
    }

    private fun updateMessagesBadge(count: Int) {
        val badge = binding.bottomNavigationView
            .getOrCreateBadge(R.id.conversationsFragment)
        if (count > 0) {
            badge.isVisible = true
            badge.number = count
            badge.maxCharacterCount = 3
        } else {
            badge.isVisible = false
        }
    }

    fun getAnalytics(): FirebaseAnalytics = analytics

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}