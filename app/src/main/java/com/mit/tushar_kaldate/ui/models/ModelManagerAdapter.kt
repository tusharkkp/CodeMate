package com.mit.tushar_kaldate.ui.models

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mit.tushar_kaldate.R
import com.mit.tushar_kaldate.ai.local.ModelTierInfo
import com.mit.tushar_kaldate.databinding.ItemModelCatalogBinding
import java.util.Locale

class ModelManagerAdapter(
    private val onDownloadClicked: (ModelTierInfo) -> Unit,
    private val onCancelClicked: (ModelTierInfo) -> Unit,
    private val onDeleteClicked: (ModelTierInfo) -> Unit,
    private val onTestClicked: (ModelTierInfo) -> Unit
) : ListAdapter<CatalogItemUiState, ModelManagerAdapter.ModelViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val binding = ItemModelCatalogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ModelViewHolder(private val binding: ItemModelCatalogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatalogItemUiState) {
            val tier = item.tierInfo
            val context = binding.root.context

            binding.tvModelName.text = tier.displayName
            binding.tvModelDescription.text = tier.description
            binding.chipRecommended.isVisible = item.isRecommended

            val sizeStr = formatBytes(tier.sizeBytes)
            val ramStr = formatBytes(tier.minRamBytes)
            binding.tvModelSpecs.text = context.getString(
                R.string.models_specs_format,
                sizeStr,
                ramStr,
                tier.license
            )

            when {
                item.isDownloading -> {
                    binding.layoutDownloadProgress.isVisible = true
                    binding.progressDownload.progress = item.downloadProgress
                    binding.tvDownloadProgressText.text = context.getString(
                        R.string.models_status_downloading,
                        item.downloadProgress
                    )

                    binding.btnDownload.isVisible = false
                    binding.btnDeleteModel.isVisible = false
                    binding.btnTestModel.isVisible = false

                    binding.ivStatusIcon.setImageResource(R.drawable.ic_refresh)
                    val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr.colorPrimary
                    )
                    binding.ivStatusIcon.setColorFilter(primaryColor)
                    binding.tvStatusText.text = context.getString(
                        R.string.models_status_downloading,
                        item.downloadProgress
                    )
                }
                item.isInstalled -> {
                    binding.layoutDownloadProgress.isVisible = false
                    binding.btnDownload.isVisible = false
                    binding.btnDeleteModel.isVisible = true
                    binding.btnTestModel.isVisible = true

                    binding.ivStatusIcon.setImageResource(R.drawable.ic_check)
                    val primaryColor = com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr.colorPrimary
                    )
                    binding.ivStatusIcon.setColorFilter(primaryColor)
                    binding.tvStatusText.text = context.getString(R.string.models_status_installed)
                }
                else -> {
                    binding.layoutDownloadProgress.isVisible = false
                    binding.btnDownload.isVisible = true
                    binding.btnDeleteModel.isVisible = false
                    binding.btnTestModel.isVisible = false

                    binding.ivStatusIcon.setImageResource(R.drawable.ic_circle_outline)
                    val outlineColor = com.google.android.material.color.MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr.colorOutline
                    )
                    binding.ivStatusIcon.setColorFilter(outlineColor)
                    binding.tvStatusText.text = context.getString(R.string.models_status_not_downloaded)
                }
            }

            binding.btnDownload.setOnClickListener { onDownloadClicked(tier) }
            binding.btnCancelDownload.setOnClickListener { onCancelClicked(tier) }
            binding.btnDeleteModel.setOnClickListener { onDeleteClicked(tier) }
            binding.btnTestModel.setOnClickListener { onTestClicked(tier) }
        }

        private fun formatBytes(bytes: Long): String {
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            return if (gb >= 1.0) {
                String.format(Locale.US, "%.1f GB", gb)
            } else {
                val mb = bytes.toDouble() / (1024 * 1024)
                String.format(Locale.US, "%.0f MB", mb)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<CatalogItemUiState>() {
        override fun areItemsTheSame(
            oldItem: CatalogItemUiState,
            newItem: CatalogItemUiState
        ): Boolean = oldItem.tierInfo.id == newItem.tierInfo.id

        override fun areContentsTheSame(
            oldItem: CatalogItemUiState,
            newItem: CatalogItemUiState
        ): Boolean = oldItem == newItem
    }
}
