package com.mit.tushar_kaldate.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.ViewLoadingStateBinding

class LoadingStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewLoadingStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun setMessage(message: UiText) {
        binding.tvLoadingMessage.text = message.asString(context)
    }

    fun setMessage(message: String) {
        binding.tvLoadingMessage.text = message
    }
}
