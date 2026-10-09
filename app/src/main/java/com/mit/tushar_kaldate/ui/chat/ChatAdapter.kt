package com.mit.tushar_kaldate.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.core.ui.MarkdownRenderer
import com.mit.tushar_kaldate.data.model.EngineType
import com.mit.tushar_kaldate.data.model.MessageRole
import com.mit.tushar_kaldate.databinding.ItemChatMessageAssistantBinding
import com.mit.tushar_kaldate.databinding.ItemChatMessageUserBinding

data class ChatMessageUiModel(
    val id: Long,
    val role: MessageRole,
    val content: String,
    val engineUsed: EngineType? = null,
    val isStreaming: Boolean = false,
    val isLastMessage: Boolean = false
)

class ChatAdapter(
    private val onCopyClicked: (String) -> Unit,
    private val onRegenerateClicked: () -> Unit
) : ListAdapter<ChatMessageUiModel, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<ChatMessageUiModel>() {
            override fun areItemsTheSame(
                oldItem: ChatMessageUiModel,
                newItem: ChatMessageUiModel
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ChatMessageUiModel,
                newItem: ChatMessageUiModel
            ): Boolean = oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).role) {
            MessageRole.USER -> VIEW_TYPE_USER
            MessageRole.ASSISTANT, MessageRole.SYSTEM -> VIEW_TYPE_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> {
                val binding = ItemChatMessageUserBinding.inflate(inflater, parent, false)
                UserViewHolder(binding)
            }
            else -> {
                val binding = ItemChatMessageAssistantBinding.inflate(inflater, parent, false)
                AssistantViewHolder(binding, onCopyClicked, onRegenerateClicked)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(item)
            is AssistantViewHolder -> holder.bind(item)
        }
    }

    class UserViewHolder(
        private val binding: ItemChatMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessageUiModel) {
            binding.tvUserContent.text = item.content
        }
    }

    class AssistantViewHolder(
        private val binding: ItemChatMessageAssistantBinding,
        private val onCopyClicked: (String) -> Unit,
        private val onRegenerateClicked: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessageUiModel) {
            val context = binding.root.context

            // Render Markdown
            MarkdownRenderer.get(context).render(binding.tvAssistantContent, item.content)

            // Engine Badge
            val badgeText = when (item.engineUsed) {
                EngineType.CLOUD -> context.getString(R.string.chat_engine_badge_cloud)
                EngineType.LOCAL -> context.getString(R.string.chat_engine_badge_local)
                else -> context.getString(R.string.chat_engine_badge_cloud)
            }
            binding.tvEngineBadge.text = badgeText

            // Streaming indicator
            binding.layoutStreamingIndicator.visibility =
                if (item.isStreaming) View.VISIBLE else View.GONE

            // Regenerate button
            binding.btnRegenerate.visibility =
                if (item.isLastMessage && !item.isStreaming) View.VISIBLE else View.GONE

            binding.btnCopy.setOnClickListener {
                onCopyClicked(item.content)
            }

            binding.btnRegenerate.setOnClickListener {
                onRegenerateClicked()
            }
        }
    }
}
