package com.mit.tushar_kaldate.ui.learn

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentLearnBinding

class LearnFragment : Fragment() {

    private var _binding: FragmentLearnBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.learnEmptyState.setTitle(UiText.DynamicString("Socratic Learning"))
        binding.learnEmptyState.setSubtitle(UiText.DynamicString("Step-by-step interactive tutor mode with curriculum."))
        binding.learnEmptyState.setIcon(R.drawable.ic_learn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
