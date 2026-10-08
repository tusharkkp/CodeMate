package com.mit.tushar_kaldate.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentProgressBinding

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.progressEmptyState.setTitle(UiText.DynamicString("Learning Analytics"))
        binding.progressEmptyState.setSubtitle(UiText.DynamicString("Track your mastery, study streaks, and XP points."))
        binding.progressEmptyState.setIcon(R.drawable.ic_progress)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
