package com.sally.zlauncher.ui

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sally.zlauncher.R
import com.sally.zlauncher.data.GameVersion
import com.sally.zlauncher.databinding.ItemVersionBinding

private object VersionDiff : DiffUtil.ItemCallback<GameVersion>() {
    override fun areItemsTheSame(oldItem: GameVersion, newItem: GameVersion): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: GameVersion, newItem: GameVersion): Boolean =
        oldItem == newItem
}

class VersionAdapter(
    private val onLaunch: (GameVersion) -> Unit,
    private val onDelete: (GameVersion) -> Unit
) : ListAdapter<GameVersion, VersionAdapter.Holder>(VersionDiff) {

    class Holder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val version = getItem(position)
        val b = holder.binding
        val ctx = b.root.context

        b.tvName.text = version.displayName
        b.tvVersion.text = ctx.getString(R.string.version_fmt, version.versionName)
        b.tvMeta.text = if (version.isInstalled) {
            ctx.getString(R.string.meta_installed)
        } else {
            Formatter.formatShortFileSize(ctx, version.sizeBytes)
        }
        b.btnDelete.isVisible = !version.isInstalled
        b.btnLaunch.setOnClickListener { onLaunch(version) }
        b.btnDelete.setOnClickListener { onDelete(version) }
    }
}
