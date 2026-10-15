package com.smitnk.grinder

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smitnk.grinder.data.Layer

class LayerAdapter(
    private var layers: MutableList<Layer>,
    private var activeLayerIndex: Int,
    private val onLayerClick: (Int) -> Unit,
    private val onLayerVisibilityChange: (Int, Boolean) -> Unit,
    private val onLayerLockChange: (Int, Boolean) -> Unit,
    private val onLayerOpacityChange: (Int, Float) -> Unit,
    private val onLayerLongClick: (Int) -> Unit
) : RecyclerView.Adapter<LayerAdapter.LayerViewHolder>() {

    inner class LayerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.layerCard)
        val nameText: TextView = view.findViewById(R.id.layerName)
        val visibilitySwitch: SwitchMaterial = view.findViewById(R.id.layerVisibility)
        val lockSwitch: SwitchMaterial = view.findViewById(R.id.layerLock)
        val opacitySeekBar: SeekBar = view.findViewById(R.id.layerOpacity)
        val deleteBtn: ImageButton = view.findViewById(R.id.layerDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layer, parent, false)
        return LayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        val layer = layers[position]
        val isActive = position == activeLayerIndex

        holder.nameText.text = layer.name
        holder.nameText.setTextColor(if (isActive) Color.parseColor("#FF6B35") else Color.BLACK)

        holder.card.setCardBackgroundColor(
            if (isActive) Color.parseColor("#FFF3E0") else Color.parseColor("#FAFAFA")
        )

        holder.visibilitySwitch.isChecked = layer.visible
        holder.visibilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            onLayerVisibilityChange(position, isChecked)
        }

        holder.lockSwitch.isChecked = layer.locked
        holder.lockSwitch.setOnCheckedChangeListener { _, isChecked ->
            onLayerLockChange(position, isChecked)
        }

        holder.opacitySeekBar.progress = (layer.opacity * 100).toInt()
        holder.opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) onLayerOpacityChange(position, progress / 100f)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        holder.deleteBtn.setOnClickListener {
            onLayerLongClick(position)
        }

        holder.itemView.setOnClickListener {
            onLayerClick(position)
        }

        holder.itemView.setOnLongClickListener {
            onLayerLongClick(position)
            true
        }

        // Dim if locked
        holder.itemView.alpha = if (layer.locked) 0.6f else 1.0f
    }

    override fun getItemCount(): Int = layers.size

    fun updateActiveLayer(index: Int) {
        val prevActive = activeLayerIndex
        activeLayerIndex = index
        if (prevActive != index) {
            if (prevActive in layers.indices) notifyItemChanged(prevActive)
            if (index in layers.indices) notifyItemChanged(index)
        }
    }

    fun updateLayers(newLayers: MutableList<Layer>) {
        layers = newLayers
        notifyDataSetChanged()
    }
}