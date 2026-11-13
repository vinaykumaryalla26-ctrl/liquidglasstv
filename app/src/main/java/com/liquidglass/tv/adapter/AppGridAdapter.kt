package com.liquidglass.tv.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.liquidglass.tv.databinding.ItemAppGridBinding
import com.liquidglass.tv.model.AppInfo
import com.liquidglass.tv.util.FocusHelper

class AppGridAdapter(
    private val appList: List<AppInfo>,
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemAppGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.appIcon.setImageDrawable(app.icon)
            binding.appLabel.text = app.label

            binding.cardRoot.setOnClickListener {
                onAppClick(app)
            }

            binding.cardRoot.setOnFocusChangeListener { view, hasFocus ->
                FocusHelper.applyFocusScale(view, hasFocus, scale = 1.08f, elevationDp = 10f)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    override fun getItemCount(): Int = appList.size
}
