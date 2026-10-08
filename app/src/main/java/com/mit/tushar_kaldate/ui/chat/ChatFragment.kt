package com.mit.tushar_kaldate.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

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
        binding.chatEmptyState.setTitle(UiText.DynamicString("Start a Conversation"))
        binding.chatEmptyState.setSubtitle(UiText.DynamicString("Ask about Java, Python, C++, or algorithm concepts."))
        binding.chatEmptyState.setIcon(R.drawable.ic_chat)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
