package com.liquidglass.tv.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liquidglass.tv.databinding.ItemDockAppBinding
import com.liquidglass.tv.model.AppInfo
import com.liquidglass.tv.util.FocusHelper

class DockAdapter(
    private val dockApps: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<DockAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemDockAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.dockAppIcon.setImageDrawable(app.icon)

            binding.dockItemRoot.setOnClickListener {
                onAppClick(app)
            }

            binding.dockItemRoot.setOnFocusChangeListener { view, hasFocus ->
                FocusHelper.applyFocusScale(view, hasFocus, scale = 1.14f, elevationDp = 12f)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDockAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dockApps[position])
    }

    override fun getItemCount(): Int = dockApps.size
}
