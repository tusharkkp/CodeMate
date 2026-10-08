package com.mit.tushar_kaldate

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.mit.tushar_kaldate.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
    }
}
