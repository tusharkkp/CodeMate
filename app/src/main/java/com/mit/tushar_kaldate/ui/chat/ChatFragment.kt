package com.mit.tushar_kaldate.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mit.tushar_kaldate.CodeMateApp
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.LearnerLevel
import com.mit.tushar_kaldate.databinding.FragmentChatBinding
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels {
        val app = requireActivity().application as CodeMateApp
        ChatViewModel.Factory(app.appContainer)
    }

    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupEmptyState()
        observeUiState()

        // Initialize new conversation or existing one from arguments if provided
        val convId = arguments?.getLong("conversationId", -1L)?.takeIf { it > 0 }
        viewModel.initConversation(convId)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(
            onCopyClicked = { text ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("CodeMate", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), R.string.action_copied, Toast.LENGTH_SHORT).show()
            },
            onRegenerateClicked = {
                viewModel.regenerateLastMessage()
            }
        )

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }

        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = chatAdapter
    }

    private fun setupListeners() {
        binding.btnSendMessage.setOnClickListener {
            val text = binding.etMessageInput.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessageInput.text?.clear()
            }
        }

        binding.btnStopGeneration.setOnClickListener {
            viewModel.stopGeneration()
        }

        binding.tvChatErrorBanner.setOnClickListener {
            viewModel.clearError()
        }

        binding.chipLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.chipLevel.setOnClickListener {
            showLevelDialog()
        }

        binding.chipEngine.setOnClickListener {
            showEngineDialog()
        }
    }

    private fun setupEmptyState() {
        binding.chatEmptyState.setTitle(UiText.StringResource(R.string.chat_empty_title))
        binding.chatEmptyState.setSubtitle(UiText.StringResource(R.string.chat_empty_subtitle))
        binding.chatEmptyState.setIcon(R.drawable.ic_chat)
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("Python", "Java", "C++", "Kotlin")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_language_dialog_title)
            .setItems(languages) { _, which ->
                viewModel.setLanguage(languages[which])
            }
            .show()
    }

    private fun showLevelDialog() {
        val levels = arrayOf("Beginner", "Intermediate", "Advanced")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_level_dialog_title)
            .setItems(levels) { _, which ->
                val selected = when (which) {
                    0 -> LearnerLevel.BEGINNER
                    1 -> LearnerLevel.INTERMEDIATE
                    else -> LearnerLevel.ADVANCED
                }
                viewModel.setLevel(selected)
            }
            .show()
    }

    private fun showEngineDialog() {
        val engines = arrayOf("Cloud (Gemini)", "On-Device (LiteRT)", "Auto (Cloud + Local)")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.chat_engine_dialog_title)
            .setItems(engines) { _, which ->
                val selected = when (which) {
                    0 -> EngineType.CLOUD
                    1 -> EngineType.LOCAL
                    else -> EngineType.AUTO
                }
                viewModel.setEngine(selected)
            }
            .show()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: ChatUiState) {
        // Update Chips
        binding.chipLanguage.text = state.selectedLanguage
        binding.chipLevel.text = when (state.selectedLevel) {
            LearnerLevel.BEGINNER -> "Beginner"
            LearnerLevel.INTERMEDIATE -> "Intermediate"
            LearnerLevel.ADVANCED -> "Advanced"
        }
        binding.chipEngine.text = when (state.selectedEngine) {
            EngineType.CLOUD -> getString(R.string.chat_engine_badge_cloud)
            EngineType.LOCAL -> getString(R.string.chat_engine_badge_local)
            EngineType.AUTO -> "Auto"
        }

        // Toggle Streaming / Send Controls
        if (state.isStreaming) {
            binding.btnStopGeneration.visibility = View.VISIBLE
            binding.btnSendMessage.visibility = View.GONE
        } else {
            binding.btnStopGeneration.visibility = View.GONE
            binding.btnSendMessage.visibility = View.VISIBLE
        }

        // Error Banner
        if (state.errorMessage != null) {
            binding.tvChatErrorBanner.text = state.errorMessage.asString(requireContext())
            binding.tvChatErrorBanner.visibility = View.VISIBLE
        } else {
            binding.tvChatErrorBanner.visibility = View.GONE
        }

        // Empty vs Content State
        if (state.isEmpty && !state.isStreaming) {
            binding.chatEmptyState.visibility = View.VISIBLE
            binding.rvChatMessages.visibility = View.GONE
        } else {
            binding.chatEmptyState.visibility = View.GONE
            binding.rvChatMessages.visibility = View.VISIBLE
            chatAdapter.submitList(state.messages) {
                if (state.messages.isNotEmpty()) {
                    binding.rvChatMessages.scrollToPosition(state.messages.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
