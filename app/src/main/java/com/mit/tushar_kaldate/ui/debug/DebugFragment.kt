package com.mit.tushar_kaldate.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentDebugBinding

class DebugFragment : Fragment() {

    private var _binding: FragmentDebugBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.debugCodeBlock.setCode(
            """
            for i in range(len(arr)):
                print(arr[i + 1]) # IndexError: list index out of range
            """.trimIndent(),
            language = "python"
        )
        binding.debugEmptyState.setTitle(UiText.DynamicString("AI Code Debugger"))
        binding.debugEmptyState.setSubtitle(UiText.DynamicString("Provide broken code to get root cause diagnosis and verified fixes."))
        binding.debugEmptyState.setIcon(R.drawable.ic_bug)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
