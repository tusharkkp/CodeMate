package com.mit.tushar_kaldate.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.historyEmptyState.setTitle(UiText.DynamicString("Chat History"))
        binding.historyEmptyState.setSubtitle(UiText.DynamicString("No past sessions found. Start a new conversation."))
        binding.historyEmptyState.setIcon(R.drawable.ic_history)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
