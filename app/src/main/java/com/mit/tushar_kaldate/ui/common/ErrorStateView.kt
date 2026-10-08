package com.mit.tushar_kaldate.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.ViewErrorStateBinding

class ErrorStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewErrorStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun setErrorMessage(message: UiText) {
        binding.tvErrorMessage.text = message.asString(context)
    }

    fun setErrorMessage(message: String) {
        binding.tvErrorMessage.text = message
    }

    fun setOnRetryClickListener(listener: () -> Unit) {
        binding.btnRetry.setOnClickListener { listener() }
    }
}
