package com.smitnk.grinder

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.smitnk.grinder.data.BlendMode

class BrushPropertyAdapter(
    private val onColorChange: (Int) -> Unit,
    private val onSizeChange: (Float) -> Unit,
    private val onOpacityChange: (Float) -> Unit,
    private val onHardnessChange: (Float) -> Unit,
    private val onBlendModeChange: (BlendMode) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val brushColors = arrayOf(
        Color.parseColor("#FF000000"), Color.parseColor("#FFFFFFFF"),
        Color.parseColor("#FFD32F2F"), Color.parseColor("#FFF57C00"),
        Color.parseColor("#FFF9A825"), Color.parseColor("#FF388E3C"),
        Color.parseColor("#FF00897B"), Color.parseColor("#FF1976D2"),
        Color.parseColor("#FF3F51B5"), Color.parseColor("#FF7B1FA2"),
        Color.parseColor("#FFC2185B"), Color.parseColor("#FF6D4C41"),
        Color.parseColor("#FF757575"), Color.parseColor("#FF546E7A")
    )

    companion object {
        const val TYPE_COLOR = 0
        const val TYPE_SIZE = 1
        const val TYPE_OPACITY = 2
        const val TYPE_HARDNESS = 3
        const val TYPE_BLEND = 4
    }

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_COLOR -> ColorViewHolder(inflater.inflate(R.layout.item_brush_color, parent, false))
            TYPE_SIZE -> SliderViewHolder(inflater.inflate(R.layout.item_brush_slider, parent, false), "Size", 0.5f, 200f, 4f, onSizeChange)
            TYPE_OPACITY -> SliderViewHolder(inflater.inflate(R.layout.item_brush_slider, parent, false), "Opacity", 0f, 1f, 1f, onOpacityChange)
            TYPE_HARDNESS -> SliderViewHolder(inflater.inflate(R.layout.item_brush_slider, parent, false), "Hardness", 0f, 1f, 1f, onHardnessChange)
            TYPE_BLEND -> BlendModeViewHolder(inflater.inflate(R.layout.item_brush_blend, parent, false), onBlendModeChange)
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ColorViewHolder -> holder.bind(brushColors)
            is SliderViewHolder -> holder.bind()
            is BlendModeViewHolder -> holder.bind()
        }
    }

    override fun getItemCount(): Int = 5

    inner class ColorViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val colorGrid: LinearLayout = view.findViewById(R.id.colorGrid)

        fun bind(colors: Array<Int>) {
            colorGrid.removeAllViews()
            // 2 rows of 7 colors
            for (row in 0..1) {
                val rowLayout = LinearLayout(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { gravity = Gravity.CENTER_HORIZONTAL }
                    orientation = LinearLayout.HORIZONTAL
                }
                for (col in 0..6) {
                    val idx = row * 7 + col
                    if (idx >= colors.size) break
                    val color = colors[idx]
                    val colorView = View(itemView.context).apply {
                        layoutParams = LinearLayout.LayoutParams(38, 38).apply {
                            setMargins(3, 3, 3, 3)
                        }
                        if (color == Color.WHITE) {
                            setBackgroundResource(R.drawable.color_white_bg)
                        } else {
                            setBackgroundColor(color)
                        }
                        setOnClickListener { onColorChange(color) }
                    }
                    rowLayout.addView(colorView)
                }
                colorGrid.addView(rowLayout)
            }
        }
    }

    inner class SliderViewHolder(
        view: View,
        private val label: String,
        private val min: Float,
        private val max: Float,
        private val defaultValue: Float,
        private val onChange: (Float) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val labelText: TextView = view.findViewById(R.id.sliderLabel)
        private val valueText: TextView = view.findViewById(R.id.sliderValue)
        private val slider: Slider = view.findViewById(R.id.slider)

        init {
            labelText.text = label
            slider.valueFrom = min
            slider.valueTo = max
            slider.value = defaultValue
            slider.addOnChangeListener { _, value, fromUser ->
                if (fromUser) {
                    valueText.text = if (max <= 1f) "${(value * 100).toInt()}%" else "${value.toInt()}"
                    onChange(value)
                }
            }
        }

        fun bind() {
            valueText.text = if (max <= 1f) "${(defaultValue * 100).toInt()}%" else "${defaultValue.toInt()}"
        }
    }

    inner class BlendModeViewHolder(view: View, private val onChange: (BlendMode) -> Unit) : RecyclerView.ViewHolder(view) {
        private val blendModeText: TextView = view.findViewById(R.id.blendModeText)

        fun bind() {
            blendModeText.setOnClickListener {
                val modes = BlendMode.values()
                val items = modes.map { it.name }.toTypedArray()
                androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                    .setTitle("Blend Mode")
                    .setItems(items) { _, which ->
                        onChange(modes[which])
                        blendModeText.text = modes[which].name
                    }
                    .show()
            }
            blendModeText.text = BlendMode.NORMAL.name
        }
    }
}