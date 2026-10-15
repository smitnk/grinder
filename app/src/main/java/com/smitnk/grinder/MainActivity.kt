package com.smitnk.grinder

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.smitnk.grinder.data.*
import com.smitnk.grinder.engine.DrawingEngine
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var drawingEngine: DrawingEngine
    private lateinit var layerAdapter: LayerAdapter
    private lateinit var brushPropertyAdapter: BrushPropertyAdapter

    private var currentColor = Color.BLACK
    private var currentThickness = 4.0f
    private var currentOpacity = 1.0f
    private var currentHardness = 1.0f
    private var currentBlendMode = BlendMode.NORMAL
    private var currentTool = DrawingEngine.Tool.BRUSH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingEngine = findViewById(R.id.drawingEngine)

        setupDrawingEngine()
        setupToolBar()
        setupTopAppBar()
        setupBrushPanel()
        setupLayerPanel()
        setupStatusBar()

        // Add initial layer
        drawingEngine.addLayer("Layer 1")
    }

    private fun setupDrawingEngine() {
        drawingEngine.onStrokeComplete = { stroke ->
            updateStatus("Stroke added")
        }
        drawingEngine.onStrokeStart = {
            updateStatus("Drawing...")
        }

        drawingEngine.greasePencilData = GreasePencilData().apply {
            layers.add(Layer(name = "Layer 1").apply {
                frames.add(Frame(frameNumber = 0))
            })
            activeLayerIndex = 0
            activeFrameIndex = 0
        }
    }

    private fun setupToolBar() {
        val toolBar = findViewById<com.google.android.material.navigation.NavigationView>(R.id.toolBar)
        toolBar.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tool_brush -> {
                    currentTool = DrawingEngine.Tool.BRUSH
                    drawingEngine.setTool(currentTool)
                    updateStatus("Brush selected")
                }
                R.id.tool_eraser -> {
                    currentTool = DrawingEngine.Tool.ERASER
                    drawingEngine.setTool(currentTool)
                    updateStatus("Eraser selected")
                }
                R.id.tool_eyedropper -> {
                    currentTool = DrawingEngine.Tool.EYEDROPPER
                    drawingEngine.setTool(currentTool)
                    updateStatus("Eyedropper selected")
                }
                R.id.tool_select -> {
                    currentTool = DrawingEngine.Tool.SELECT
                    drawingEngine.setTool(currentTool)
                    updateStatus("Select tool")
                }
                R.id.tool_transform -> {
                    currentTool = DrawingEngine.Tool.TRANSFORM
                    drawingEngine.setTool(currentTool)
                    updateStatus("Transform tool")
                }
                R.id.tool_fill -> {
                    currentTool = DrawingEngine.Tool.FILL
                    drawingEngine.setTool(currentTool)
                    updateStatus("Fill tool")
                }
            }
            item.isChecked = true
            true
        }
    }

    private fun setupTopAppBar() {
        val topAppBar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_new -> newFile()
                R.id.action_open -> openFile()
                R.id.action_save -> saveFile()
                R.id.action_export -> exportImage()
                R.id.action_undo -> drawingEngine.undoLastStroke()
                R.id.action_redo -> Toast.makeText(this, "Redo not implemented", Toast.LENGTH_SHORT).show()
                R.id.action_settings -> showSettings()
                R.id.action_about -> showAbout()
            }
            true
        }
    }

    private fun setupBrushPanel() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.brushPropertiesRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        brushPropertyAdapter = BrushPropertyAdapter(
            onColorChange = { color -> onColorSelected(color) },
            onSizeChange = { size -> onSizeChanged(size) },
            onOpacityChange = { opacity -> onOpacityChanged(opacity) },
            onHardnessChange = { hardness -> onHardnessChanged(hardness) },
            onBlendModeChange = { mode -> onBlendModeChanged(mode) }
        )
        recycler.adapter = brushPropertyAdapter
    }

    private fun setupLayerPanel() {
        val recycler = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.layerRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        layerAdapter = LayerAdapter(
            layers = drawingEngine.greasePencilData.layers,
            activeLayerIndex = drawingEngine.greasePencilData.activeLayerIndex,
            onLayerClick = { index -> onLayerSelected(index) },
            onLayerVisibilityChange = { index, visible -> onLayerVisibilityChanged(index, visible) },
            onLayerLockChange = { index, locked -> onLayerLockChanged(index, locked) },
            onLayerOpacityChange = { index, opacity -> onLayerOpacityChanged(index, opacity) },
            onLayerLongClick = { index -> showLayerOptions(index) }
        )
        recycler.adapter = layerAdapter

        findViewById<android.widget.ImageButton>(R.id.btnAddLayer).setOnClickListener {
            addNewLayer()
        }
    }

    private fun setupStatusBar() {
        updateStatus("Ready")
    }

    // ==================== Event Handlers ====================

    private fun onColorSelected(color: Int) {
        currentColor = color
        drawingEngine.setColor(color)
        updateStatus("Color changed")
    }

    private fun onSizeChanged(size: Float) {
        currentThickness = size
        drawingEngine.setThickness(size)
        updateStatus("Size: ${size.toInt()}px")
    }

    private fun onOpacityChanged(opacity: Float) {
        currentOpacity = opacity
        drawingEngine.setOpacity(opacity)
        updateStatus("Opacity: ${(opacity * 100).toInt()}%")
    }

    private fun onHardnessChanged(hardness: Float) {
        currentHardness = hardness
        drawingEngine.setHardness(hardness)
        updateStatus("Hardness: ${(hardness * 100).toInt()}%")
    }

    private fun onBlendModeChanged(mode: BlendMode) {
        currentBlendMode = mode
        drawingEngine.setBlendMode(mode)
        updateStatus("Blend: $mode")
    }

    private fun onLayerSelected(index: Int) {
        drawingEngine.setActiveLayer(index)
        layerAdapter.updateActiveLayer(index)
        updateStatus("Layer ${index + 1} selected")
    }

    private fun onLayerVisibilityChanged(index: Int, visible: Boolean) {
        drawingEngine.greasePencilData.layers[index].visible = visible
        drawingEngine.invalidate()
        layerAdapter.notifyItemChanged(index)
    }

    private fun onLayerLockChanged(index: Int, locked: Boolean) {
        drawingEngine.greasePencilData.layers[index].locked = locked
        layerAdapter.notifyItemChanged(index)
    }

    private fun onLayerOpacityChanged(index: Int, opacity: Float) {
        drawingEngine.greasePencilData.layers[index].opacity = opacity
        drawingEngine.invalidate()
    }

    private fun showLayerOptions(index: Int) {
        val menu = PopupMenu(this, findViewById(R.id.layerRecycler))
        menu.menu.add(0, 0, 0, "Duplicate")
        menu.menu.add(0, 1, 0, "Delete")
        menu.menu.add(0, 2, 0, "Merge Down")
        menu.menu.add(0, 3, 0, "Rename")
        menu.setOnMenuItemClickListener { item ->
            when (item.order) {
                0 -> duplicateLayer(index)
                1 -> deleteLayer(index)
                2 -> mergeDown(index)
                3 -> renameLayer(index)
            }
            true
        }
        menu.show()
    }

    // ==================== File Operations ====================

    private fun newFile() {
        if (hasUnsavedChanges()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("New File")
                .setMessage("Discard unsaved changes?")
                .setPositiveButton("Discard") { _, _ -> clearCanvas() }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            clearCanvas()
        }
    }

    private fun clearCanvas() {
        drawingEngine.greasePencilData.layers.forEach { layer ->
            layer.frames.forEach { frame ->
                frame.strokes.clear()
            }
        }
        drawingEngine.invalidate()
        updateStatus("New canvas created")
    }

    private fun hasUnsavedChanges(): Boolean {
        return drawingEngine.greasePencilData.layers.any { layer ->
            layer.frames.any { frame -> frame.strokes.isNotEmpty() }
        }
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json"))
        }
        startActivityForResult(intent, REQUEST_OPEN_FILE)
    }

    private fun saveFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "drawing_${getTimestamp()}.grinder")
        }
        startActivityForResult(intent, REQUEST_SAVE_FILE)
    }

    private fun exportImage() {
        val bitmap = drawingEngine.exportAsImage()
        val fileName = "grinder_${getTimestamp()}.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Grinder")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                updateStatus("Exported to Pictures/Grinder")
                Toast.makeText(this, "Exported: $fileName", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    // ==================== Layer Operations ====================

    private fun addNewLayer() {
        val layerCount = drawingEngine.greasePencilData.layers.size + 1
        drawingEngine.addLayer("Layer $layerCount")
        layerAdapter.notifyItemInserted(drawingEngine.greasePencilData.layers.lastIndex)
        layerAdapter.updateActiveLayer(drawingEngine.greasePencilData.activeLayerIndex)
        updateStatus("Layer $layerCount added")
    }

    private fun deleteLayer(index: Int) {
        if (drawingEngine.greasePencilData.layers.size <= 1) {
            Toast.makeText(this, "Cannot delete last layer", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Layer")
            .setMessage("Delete ${drawingEngine.greasePencilData.layers[index].name}?")
            .setPositiveButton("Delete") { _, _ ->
                drawingEngine.removeLayer(index)
                layerAdapter.notifyItemRemoved(index)
                layerAdapter.updateActiveLayer(drawingEngine.greasePencilData.activeLayerIndex)
                updateStatus("Layer deleted")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun duplicateLayer(index: Int) {
        val sourceLayer = drawingEngine.greasePencilData.layers[index]
        val newLayer = Layer(
            name = "${sourceLayer.name} Copy",
            frames = sourceLayer.frames.map { frame ->
                Frame(
                    frameNumber = frame.frameNumber,
                    strokes = frame.strokes.map { it.copy() }.toMutableList()
                ).toMutableList()
            }.toMutableList(),
            opacity = sourceLayer.opacity,
            visible = sourceLayer.visible,
            locked = sourceLayer.locked
        )
        drawingEngine.greasePencilData.layers.add(index + 1, newLayer)
        layerAdapter.notifyItemInserted(index + 1)
        updateStatus("Layer duplicated")
    }

    private fun mergeDown(index: Int) {
        if (index >= drawingEngine.greasePencilData.layers.size - 1) {
            Toast.makeText(this, "Cannot merge bottom layer", Toast.LENGTH_SHORT).show()
            return
        }
        val upperLayer = drawingEngine.greasePencilData.layers[index]
        val lowerLayer = drawingEngine.greasePencilData.layers[index + 1]

        // Merge frames
        for (i in upperLayer.frames.indices) {
            val upperFrame = upperLayer.frames[i]
            val lowerFrame = if (i < lowerLayer.frames.size) lowerLayer.frames[i] else {
                val f = Frame(frameNumber = i)
                lowerLayer.frames.add(f)
                f
            }
            lowerFrame.strokes.addAll(upperFrame.strokes)
        }

        drawingEngine.greasePencilData.layers.removeAt(index)
        layerAdapter.notifyItemRemoved(index)
        layerAdapter.notifyItemChanged(index)
        updateStatus("Merged down")
    }

    private fun renameLayer(index: Int) {
        val layer = drawingEngine.greasePencilData.layers[index]
        val input = TextInputEditText(this).apply {
            setText(layer.name)
            selectAll()
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Rename Layer")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    layer.name = newName
                    layerAdapter.notifyItemChanged(index)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ==================== Dialogs ====================

    private fun showSettings() {
        Toast.makeText(this, "Settings coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Grinder")
            .setMessage("2D Drawing App with Grease Pencil Architecture\n\nVersion 1.0\nBuilt with Kotlin & Material3")
            .setPositiveButton("OK", null)
            .show()
    }

    // ==================== Helpers ====================

    private fun updateStatus(message: String) {
        runOnUiThread {
            findViewById<TextView>(R.id.statusText).text = message
        }
    }

    companion object {
        const val REQUEST_OPEN_FILE = 1001
        const val REQUEST_SAVE_FILE = 1002
    }
}