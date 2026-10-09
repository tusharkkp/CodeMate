package com.mit.tushar_kaldate.ui.models

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.mit.tushar_kaldate.CodeMateApp
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.ai.local.DeviceCapabilityChecker
import com.mit.tushar_kaldate.ai.local.ModelTierInfo
import com.mit.tushar_kaldate.databinding.FragmentModelManagerBinding
import kotlinx.coroutines.launch

class ModelManagerFragment : Fragment() {

    private var _binding: FragmentModelManagerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ModelManagerViewModel by viewModels {
        val app = requireActivity().application as CodeMateApp
        ModelManagerViewModel.Factory(
            context = requireContext().applicationContext,
            modelManager = app.container.modelManager,
            capabilityChecker = DeviceCapabilityChecker(requireContext().applicationContext),
            localEngine = app.container.liteRtLocalEngine
        )
    }

    private lateinit var modelAdapter: ModelManagerAdapter

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { handleImportedUri(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModelManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        modelAdapter = ModelManagerAdapter(
            onDownloadClicked = { tier -> handleDownloadClick(tier) },
            onCancelClicked = { tier -> viewModel.cancelDownload(tier) },
            onDeleteClicked = { tier -> showDeleteConfirmation(tier) },
            onTestClicked = { _ -> viewModel.runTestPrompt() }
        )
        binding.rvModelCatalog.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = modelAdapter
        }
    }

    private fun setupListeners() {
        binding.switchWifiOnly.setOnClickListener {
            viewModel.setWifiOnly(binding.switchWifiOnly.isChecked)
        }

        binding.btnImportModel.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        binding.btnRunQuickTest.setOnClickListener {
            viewModel.runTestPrompt()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderUiState(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is ModelManagerEvent.ShowMessage -> {
                                Snackbar.make(binding.root, event.text.asString(requireContext()), Snackbar.LENGTH_SHORT).show()
                            }
                            is ModelManagerEvent.ShowError -> {
                                Snackbar.make(binding.root, event.text.asString(requireContext()), Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderUiState(state: ModelManagerUiState) {
        state.capability?.let { cap ->
            binding.chipRam.text = getString(R.string.models_ram_detected, cap.totalRamGb)
            binding.chipStorage.text = getString(R.string.models_storage_free, cap.freeStorageGb)

            binding.cardHardwareWarning.isVisible = !cap.warningMessage.isNullOrBlank()
            binding.tvHardwareWarning.text = cap.warningMessage.orEmpty()
        }

        if (binding.switchWifiOnly.isChecked != state.wifiOnly) {
            binding.switchWifiOnly.isChecked = state.wifiOnly
        }

        // Active model and test card
        if (state.activeModel != null) {
            binding.cardTestModel.isVisible = true
            binding.tvActiveModelTitle.text = getString(
                R.string.chat_engine_badge_local
            ) + ": ${state.activeModel.displayName}"

            binding.progressTestInference.isVisible = state.isTestingModel
            binding.btnRunQuickTest.isEnabled = !state.isTestingModel

            if (!state.testResponse.isNullOrBlank()) {
                binding.tvTestResponse.isVisible = true
                binding.tvTestResponse.text = state.testResponse
            } else {
                binding.tvTestResponse.isVisible = false
            }
        } else {
            binding.cardTestModel.isVisible = false
        }

        modelAdapter.submitList(state.models)
    }

    private fun handleDownloadClick(tier: ModelTierInfo) {
        if (tier.isGated) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(tier.displayName)
                .setMessage("${tier.displayName} is subject to the ${tier.license}. Ensure you have agreed to the license before downloading, or import a pre-downloaded model file.")
                .setPositiveButton(R.string.models_btn_download) { _, _ ->
                    viewModel.startDownload(tier)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            viewModel.startDownload(tier)
        }
    }

    private fun showDeleteConfirmation(tier: ModelTierInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.models_delete_confirm_title)
            .setMessage(getString(R.string.models_delete_confirm_message, tier.displayName))
            .setPositiveButton(R.string.models_btn_delete) { _, _ ->
                viewModel.deleteModel(tier)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun handleImportedUri(uri: Uri) {
        var filename = "imported_model.litertlm"
        val cursor: Cursor? = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    filename = it.getString(nameIndex)
                }
            }
        }
        viewModel.importModelFromUri(uri, filename)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
