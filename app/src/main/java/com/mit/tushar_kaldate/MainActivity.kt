package com.mit.tushar_kaldate

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.mit.tushar_kaldate.data.prefs.ThemeMode
import com.mit.tushar_kaldate.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as CodeMateApp

        // Handle edge-to-edge status bar insets for Top App Bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.topAppBar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBarInsets.top
            }
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_chat,
                R.id.navigation_learn,
                R.id.navigation_quiz,
                R.id.navigation_progress
            )
        )
        binding.topAppBar.setupWithNavController(navController, appBarConfiguration)

        // Connect BottomNavigationView to NavController
        binding.bottomNav.setupWithNavController(navController)

        // Top App Bar Menu Actions
        binding.topAppBar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_search,
                R.id.navigation_history,
                R.id.navigation_settings -> {
                    navController.navigate(menuItem.itemId)
                    true
                }
                else -> false
            }
        }

        // Toggle UI elements for Auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_login, R.id.navigation_register -> {
                    binding.bottomNav.visibility = View.GONE
                    binding.topAppBar.visibility = View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = View.VISIBLE
                    binding.topAppBar.visibility = View.VISIBLE
                }
            }
        }

        // Route Guard & Theme Observation
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    app.container.authRepository.isLoggedIn.collect { isLoggedIn ->
                        val currentDestId = navController.currentDestination?.id
                        if (!isLoggedIn) {
                            if (currentDestId != R.id.navigation_login && currentDestId != R.id.navigation_register) {
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, true)
                                    .build()
                                navController.navigate(R.id.navigation_login, null, navOptions)
                            }
                        }
                    }
                }

                launch {
                    app.container.settingsStore.themeMode.collect { mode ->
                        val targetNightMode = when (mode) {
                            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                        }
                        if (AppCompatDelegate.getDefaultNightMode() != targetNightMode) {
                            AppCompatDelegate.setDefaultNightMode(targetNightMode)
                        }
                    }
                }
            }
        }
    }
}
