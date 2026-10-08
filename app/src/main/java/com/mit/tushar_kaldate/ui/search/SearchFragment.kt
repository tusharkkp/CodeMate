package com.mit.tushar_kaldate.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchEmptyState.setTitle(UiText.DynamicString("Search CodeMate"))
        binding.searchEmptyState.setSubtitle(UiText.DynamicString("Search messages, quizzes, topics, and saved snippets."))
        binding.searchEmptyState.setIcon(R.drawable.ic_search)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
