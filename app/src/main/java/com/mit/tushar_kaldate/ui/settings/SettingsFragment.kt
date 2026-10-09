package com.mit.tushar_kaldate.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mit.tushar_kaldate.CodeMateApp
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.prefs.ThemeMode
import com.mit.tushar_kaldate.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels {
        val app = requireActivity().application as CodeMateApp
        SettingsViewModel.Factory(
            app.container.authRepository,
            app.container.settingsStore,
            app.container.chatRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.cardModelManager.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_models)
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbThemeLight -> ThemeMode.LIGHT
                R.id.rbThemeDark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            viewModel.setTheme(mode)
        }

        binding.rgEngine.setOnCheckedChangeListener { _, checkedId ->
            val engine = when (checkedId) {
                R.id.rbEngineCloud -> EngineType.CLOUD
                R.id.rbEngineLocal -> EngineType.LOCAL
                else -> EngineType.AUTO
            }
            viewModel.setDefaultEngine(engine)
        }

        binding.btnClearChatData.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_clear_data_title)
                .setMessage(R.string.settings_clear_data_confirm)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    viewModel.clearChatData()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.settings_logout_title)
                .setMessage(R.string.settings_logout_confirm)
                .setPositiveButton(R.string.action_logout) { _, _ ->
                    viewModel.logout()
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        // Profile
                        val name = state.currentUser?.name ?: getString(R.string.auth_guest_badge)
                        val email = state.currentUser?.email.orEmpty()
                        binding.tvProfileName.text = name
                        binding.tvProfileEmail.text = email
                        binding.chipGuestBadge.isVisible = state.isGuest

                        // Theme
                        when (state.themeMode) {
                            ThemeMode.SYSTEM -> if (binding.rgTheme.checkedRadioButtonId != R.id.rbThemeSystem) {
                                binding.rbThemeSystem.isChecked = true
                            }
                            ThemeMode.LIGHT -> if (binding.rgTheme.checkedRadioButtonId != R.id.rbThemeLight) {
                                binding.rbThemeLight.isChecked = true
                            }
                            ThemeMode.DARK -> if (binding.rgTheme.checkedRadioButtonId != R.id.rbThemeDark) {
                                binding.rbThemeDark.isChecked = true
                            }
                        }

                        // Engine
                        when (state.defaultEngine) {
                            EngineType.AUTO -> if (binding.rgEngine.checkedRadioButtonId != R.id.rbEngineAuto) {
                                binding.rbEngineAuto.isChecked = true
                            }
                            EngineType.CLOUD -> if (binding.rgEngine.checkedRadioButtonId != R.id.rbEngineCloud) {
                                binding.rbEngineCloud.isChecked = true
                            }
                            EngineType.LOCAL -> if (binding.rgEngine.checkedRadioButtonId != R.id.rbEngineLocal) {
                                binding.rbEngineLocal.isChecked = true
                            }
                        }

                        // Gemini API Key Status
                        binding.tvGeminiKeyStatus.text = if (state.isGeminiKeyConfigured) {
                            getString(R.string.settings_gemini_key_configured)
                        } else {
                            getString(R.string.settings_gemini_key_missing)
                        }
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is SettingsEvent.NavigateToLogin -> {
                                findNavController().navigate(R.id.action_settings_to_login)
                            }
                            is SettingsEvent.ShowMessage -> {
                                Snackbar.make(
                                    binding.root,
                                    event.message.asString(requireContext()),
                                    Snackbar.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
