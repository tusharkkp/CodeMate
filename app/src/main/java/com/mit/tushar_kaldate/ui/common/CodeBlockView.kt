package com.mit.tushar_kaldate.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.databinding.ViewCodeBlockBinding

class CodeBlockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewCodeBlockBinding.inflate(LayoutInflater.from(context), this, true)
    private var codeText: String = ""

    init {
        binding.btnCopyCode.setOnClickListener {
            if (codeText.isNotEmpty()) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("code", codeText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, R.string.action_copied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setCode(code: String, language: String = "code") {
        codeText = code
        binding.tvCodeContent.text = code
        binding.tvCodeLanguage.text = language.lowercase()
    }
}
