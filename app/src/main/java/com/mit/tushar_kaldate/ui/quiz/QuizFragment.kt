package com.mit.tushar_kaldate.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentQuizBinding

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.quizEmptyState.setTitle(UiText.DynamicString("Quiz Generator"))
        binding.quizEmptyState.setSubtitle(UiText.DynamicString("Generate customized programming quizzes on demand."))
        binding.quizEmptyState.setIcon(R.drawable.ic_quiz)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
