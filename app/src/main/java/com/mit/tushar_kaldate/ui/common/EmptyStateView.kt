package com.mit.tushar_kaldate.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.mit.tushar_kaldate.core.ui.UiText
import com.mit.tushar_kaldate.databinding.ViewEmptyStateBinding

class EmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewEmptyStateBinding.inflate(LayoutInflater.from(context), this, true)

    fun setTitle(title: UiText) {
        binding.tvEmptyTitle.text = title.asString(context)
    }

    fun setTitle(title: String) {
        binding.tvEmptyTitle.text = title
    }

    fun setSubtitle(subtitle: UiText?) {
        if (subtitle != null) {
            binding.tvEmptySubtitle.visibility = View.VISIBLE
            binding.tvEmptySubtitle.text = subtitle.asString(context)
        } else {
            binding.tvEmptySubtitle.visibility = View.GONE
        }
    }

    fun setIcon(@DrawableRes iconRes: Int) {
        binding.ivEmptyIcon.setImageResource(iconRes)
    }
}
