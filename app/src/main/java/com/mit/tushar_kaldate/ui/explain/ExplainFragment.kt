package com.mit.tushar_kaldate.ui.explain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentExplainBinding

class ExplainFragment : Fragment() {

    private var _binding: FragmentExplainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExplainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.codeBlockSample.setCode(
            """
            fun fibonacci(n: Int): Int {
                return if (n <= 1) n else fibonacci(n - 1) + fibonacci(n - 2)
            }
            """.trimIndent(),
            language = "kotlin"
        )
        binding.explainEmptyState.setTitle(UiText.DynamicString("Code Explanation"))
        binding.explainEmptyState.setSubtitle(UiText.DynamicString("Paste code to receive an overview, line-by-line breakdown, and Big-O analysis."))
        binding.explainEmptyState.setIcon(R.drawable.ic_code)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
