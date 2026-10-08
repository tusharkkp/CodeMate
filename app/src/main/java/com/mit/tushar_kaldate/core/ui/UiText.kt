package com.mit.tushar_kaldate.core.ui

import android.content.Context
import androidx.annotation.StringRes

/**
 * Encapsulates UI strings to safely handle either runtime dynamic strings
 * or localized Android string resources in ViewModels without leaking Context.
 */
sealed class UiText {
    data class DynamicString(val value: String) : UiText()
    class StringResource(@param:StringRes val resId: Int, vararg val args: Any) : UiText() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StringResource
            return resId == other.resId && args.contentEquals(other.args)
        }

        override fun hashCode(): Int {
            var result = resId
            result = 31 * result + args.contentHashCode()
            return result
        }
    }

    fun asString(context: Context): String {
        return when (this) {
            is DynamicString -> value
            is StringResource -> {
                if (args.isEmpty()) {
                    context.getString(resId)
                } else {
                    context.getString(resId, *args)
                }
            }
        }
    }
}
