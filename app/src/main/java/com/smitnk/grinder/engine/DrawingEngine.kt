package com.smitnk.grinder.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.smitnk.grinder.data.BlendMode
import com.smitnk.grinder.data.CapStyle
import com.smitnk.grinder.data.Frame
import com.smitnk.grinder.data.GreasePencilData
import com.smitnk.grinder.data.JoinStyle
import com.smitnk.grinder.data.Layer
import com.smitnk.grinder.data.Stroke
import com.smitnk.grinder.data.StrokePoint
import kotlin.math.min

/**
 * Drawing Engine - Handles rendering and stroke management
 * Inspired by Blender's Grease Pencil draw engine
 */
class DrawingEngine(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isDither = true
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val path = Path()
    private val tempPath = Path()

    // Current stroke being drawn
    private var currentStroke: Stroke? = null
    private var currentStrokePoints = mutableListOf<PointF>()

    // Drawing state
    private var isDrawing = false
    private var currentTool: Tool = Tool.BRUSH
    private var currentColor = Color.BLACK
    private var currentThickness = 4.0f
    private var currentHardness = 1.0f
    private var currentOpacity = 1.0f
    private var currentBlendMode = BlendMode.NORMAL
    private var currentCapStyle = CapStyle.ROUND
    private var currentJoinStyle = JoinStyle.ROUND

    // Canvas transform
    private var canvasScale = 1.0f
    private var canvasOffsetX = 0.0f
    private var canvasOffsetY = 0.0f

    // Layer management
    var greasePencilData: GreasePencilData = GreasePencilData()
        set(value) {
            field = value
            invalidate()
        }

    // Callbacks
    var onStrokeComplete: ((Stroke) -> Unit)? = null
    var onStrokeStart: (() -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    enum class Tool {
        BRUSH, ERASER, EYEDROPPER, SELECT, TRANSFORM, FILL
    }

    // ==================== Public API ====================

    fun setTool(tool: Tool) {
        currentTool = tool
    }

    fun setColor(color: Int) {
        currentColor = color
    }

    fun setThickness(thickness: Float) {
        currentThickness = thickness.coerceIn(0.5f, 200.0f)
    }

    fun setHardness(hardness: Float) {
        currentHardness = hardness.coerceIn(0.0f, 1.0f)
    }

    fun setOpacity(opacity: Float) {
        currentOpacity = opacity.coerceIn(0.0f, 1.0f)
    }

    fun setBlendMode(mode: BlendMode) {
        currentBlendMode = mode
    }

    fun setCanvasTransform(scale: Float, offsetX: Float, offsetY: Float) {
        canvasScale = scale
        canvasOffsetX = offsetX
        canvasOffsetY = offsetY
        invalidate()
    }

    fun getCanvasTransform(): Triple<Float, Float, Float> {
        return Triple(canvasScale, canvasOffsetX, canvasOffsetY)
    }

    fun addLayer(name: String = "Layer"): Layer {
        val layer = Layer(name = name)
        greasePencilData.layers.add(layer)
        greasePencilData.activeLayerIndex = greasePencilData.layers.lastIndex
        addDefaultFrameToLayer(layer)
        invalidate()
        return layer
    }

    fun removeLayer(index: Int) {
        if (greasePencilData.layers.size > 1 && index in greasePencilData.layers.indices) {
            greasePencilData.layers.removeAt(index)
            greasePencilData.activeLayerIndex = min(greasePencilData.activeLayerIndex, greasePencilData.layers.size - 1)
            invalidate()
        }
    }

    fun setActiveLayer(index: Int) {
        if (index in greasePencilData.layers.indices) {
            greasePencilData.activeLayerIndex = index
            invalidate()
        }
    }

    fun addFrameToActiveLayer(): Frame? {
        val activeLayer = getActiveLayer() ?: return null
        val frame = Frame(frameNumber = activeLayer.frames.size)
        activeLayer.frames.add(frame)
        greasePencilData.activeFrameIndex = activeLayer.frames.lastIndex
        invalidate()
        return frame
    }

    fun setActiveFrame(frameIndex: Int) {
        val activeLayer = getActiveLayer() ?: return
        if (frameIndex in activeLayer.frames.indices) {
            greasePencilData.activeFrameIndex = frameIndex
            invalidate()
        }
    }

    fun clearActiveFrame() {
        val frame = getActiveFrame()
        frame?.strokes?.clear()
        invalidate()
    }

    fun undoLastStroke() {
        val frame = getActiveFrame()
        frame?.strokes?.removeLast()
        invalidate()
    }

    fun exportAsImage(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawLayers(canvas)
        return bitmap
    }

    // ==================== Touch Handling ====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = (event.x - canvasOffsetX) / canvasScale
        val y = (event.y - canvasOffsetY) / canvasScale

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(x, y, event.pressure)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(x, y, event.pressure)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleTouchUp()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(x: Float, y: Float, pressure: Float) {
        when (currentTool) {
            Tool.BRUSH -> startStroke(x, y, pressure)
            Tool.ERASER -> startEraserStroke(x, y, pressure)
            Tool.EYEDROPPER -> pickColor(x, y)
            Tool.SELECT -> startSelection(x, y)
            Tool.TRANSFORM -> startTransform(x, y)
            Tool.FILL -> fillArea(x, y)
        }
    }

    private fun handleTouchMove(x: Float, y: Float, pressure: Float) {
        if (isDrawing && currentStroke != null) {
            addPointToStroke(x, y, pressure)
            invalidate()
        }
    }

    private fun handleTouchUp() {
        if (isDrawing && currentStroke != null) {
            finishStroke()
        }
        isDrawing = false
    }

    // ==================== Stroke Management ====================

    private fun startStroke(x: Float, y: Float, pressure: Float) {
        val activeFrame = getActiveFrame() ?: return
        isDrawing = true
        currentStrokePoints.clear()

        currentStroke = Stroke(
            color = currentColor,
            thickness = currentThickness,
            hardness = currentHardness,
            opacity = currentOpacity,
            blendMode = currentBlendMode,
            capStyle = currentCapStyle,
            joinStyle = currentJoinStyle
        )

        addPointToStroke(x, y, pressure)
        onStrokeStart?.invoke()
    }

    private fun startEraserStroke(x: Float, y: Float, pressure: Float) {
        val activeFrame = getActiveFrame() ?: return
        isDrawing = true
        currentStrokePoints.clear()

        currentStroke = Stroke(
            color = Color.TRANSPARENT,
            thickness = currentThickness * 2,
            hardness = 1.0f,
            opacity = 1.0f,
            blendMode = BlendMode.NORMAL,
            capStyle = CapStyle.ROUND,
            joinStyle = JoinStyle.ROUND
        )

        addPointToStroke(x, y, pressure)
    }

    private fun addPointToStroke(x: Float, y: Float, pressure: Float) {
        currentStrokePoints.add(PointF(x, y))
        currentStroke?.points?.add(StrokePoint(
            x = x,
            y = y,
            pressure = pressure * currentHardness,
            strength = pressure
        ))
    }

    private fun finishStroke() {
        currentStroke?.let { stroke ->
            if (stroke.points.size >= 2) {
                smoothStroke(stroke)
                val activeFrame = getActiveFrame()
                activeFrame?.strokes?.add(stroke)
                onStrokeComplete?.invoke(stroke)
            }
        }
        currentStroke = null
        currentStrokePoints.clear()
    }

    private fun smoothStroke(stroke: Stroke) {
        if (stroke.points.size < 3) return

        val smoothed = mutableListOf<StrokePoint>()
        val points = stroke.points

        // Catmull-Rom spline smoothing
        smoothed.add(points[0])

        for (i in 1 until points.size - 1) {
            val p0 = points[i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i + 2 < points.size) points[i + 2] else p2

            // Add interpolated points
            for (t in 0..2) {
                val tt = t / 3.0f
                val x = catmullRom(p0.x, p1.x, p2.x, p3.x, tt)
                val y = catmullRom(p0.y, p1.y, p2.y, p3.y, tt)
                val pressure = catmullRom(p0.pressure, p1.pressure, p2.pressure, p3.pressure, tt)
                val strength = catmullRom(p0.strength, p1.strength, p2.strength, p3.strength, tt)

                smoothed.add(StrokePoint(x, y, pressure, strength))
            }
        }

        smoothed.add(points.last())
        stroke.points.clear()
        stroke.points.addAll(smoothed)
    }

    private fun catmullRom(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * ((2.0f * p1) +
                (-p0 + p2) * t +
                (2.0f * p0 - 5.0f * p1 + 4.0f * p2 - p3) * t2 +
                (-p0 + 3.0f * p1 - 3.0f * p2 + p3) * t3)
    }

    // ==================== Rendering ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLayers(canvas)
        drawCurrentStroke(canvas)
    }

    private fun drawLayers(canvas: Canvas) {
        canvas.save()
        canvas.scale(canvasScale, canvasScale)
        canvas.translate(canvasOffsetX, canvasOffsetY)

        // Draw background
        canvas.drawColor(Color.WHITE)

        // Draw layers in order
        for (layer in greasePencilData.layers) {
            if (!layer.visible) continue

            canvas.saveLayerAlpha(null, (layer.opacity * 255).toInt(), Canvas.HAS_ALPHA_LAYER_SAVE_FLAG)

            val frame = getFrameForLayer(layer)
            frame?.strokes?.forEach { stroke ->
                drawStroke(canvas, stroke, layer.opacity)
            }

            canvas.restore()
        }

        canvas.restore()
    }

    private fun getFrameForLayer(layer: Layer): Frame? {
        if (layer.frames.isEmpty()) return null
        val index = greasePencilData.activeFrameIndex.coerceAtMost(layer.frames.size - 1)
        return if (index >= 0) layer.frames[index] else layer.frames[0]
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke, layerOpacity: Float) {
        if (stroke.points.size < 2) return

        path.reset()

        val points = stroke.points
        val firstPoint = points[0]
        path.moveTo(firstPoint.x, firstPoint.y)

        // Build path with quadratic curves for smooth rendering
        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2
            val midY = (current.y + next.y) / 2
            path.quadTo(current.x, current.y, midX, midY)
        }

        if (points.size >= 2) {
            val last = points.last()
            path.lineTo(last.x, last.y)
        }

        // Configure paint
        paint.color = stroke.color
        paint.strokeWidth = stroke.thickness * canvasScale
        paint.alpha = (stroke.opacity * layerOpacity * 255).toInt()
        paint.strokeCap = when (stroke.capStyle) {
            CapStyle.ROUND -> Paint.Cap.ROUND
            CapStyle.SQUARE -> Paint.Cap.SQUARE
            else -> Paint.Cap.BUTT
        }
        paint.strokeJoin = when (stroke.joinStyle) {
            JoinStyle.ROUND -> Paint.Join.ROUND
            JoinStyle.BEVEL -> Paint.Join.BEVEL
            else -> Paint.Join.MITER
        }

        // Apply blend mode (API 29+ uses BlendMode, older uses PorterDuff via xfermode)
        if (stroke.color == Color.TRANSPARENT) {
            // Eraser
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.blendMode = android.graphics.BlendMode.CLEAR
            } else {
                @Suppress("DEPRECATION")
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            }
            paint.color = Color.TRANSPARENT
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.blendMode = when (stroke.blendMode) {
                    BlendMode.ADD -> android.graphics.BlendMode.PLUS
                    BlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                    BlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                    BlendMode.OVERLAY -> android.graphics.BlendMode.OVERLAY
                    else -> android.graphics.BlendMode.SRC_OVER
                }
            } else {
                @Suppress("DEPRECATION")
                paint.xfermode = when (stroke.blendMode) {
                    BlendMode.ADD -> PorterDuffXfermode(PorterDuff.Mode.ADD)
                    BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                    BlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                    BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
                    else -> null
                }
            }
        }

        canvas.drawPath(path, paint)
    }

    private fun drawCurrentStroke(canvas: Canvas) {
        if (!isDrawing || currentStroke == null || currentStrokePoints.size < 2) return

        canvas.save()
        canvas.scale(canvasScale, canvasScale)
        canvas.translate(canvasOffsetX, canvasOffsetY)

        path.reset()
        val points = currentStrokePoints
        path.moveTo(points[0].x, points[0].y)

        for (i in 1 until points.size - 1) {
            val current = points[i]
            val next = points[i + 1]
            val midX = (current.x + next.x) / 2
            val midY = (current.y + next.y) / 2
            path.quadTo(current.x, current.y, midX, midY)
        }

        if (points.size >= 2) {
            val last = points.last()
            path.lineTo(last.x, last.y)
        }

        paint.color = currentColor
        paint.strokeWidth = currentThickness * canvasScale
        paint.alpha = (currentOpacity * 255).toInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = android.graphics.BlendMode.SRC_OVER
        } else {
            @Suppress("DEPRECATION")
            paint.xfermode = null
        }

        canvas.drawPath(path, paint)
        canvas.restore()
    }

    // ==================== Helper Methods ====================

    private fun getActiveLayer(): Layer? {
        return if (greasePencilData.activeLayerIndex >= 0 &&
            greasePencilData.activeLayerIndex < greasePencilData.layers.size) {
            greasePencilData.layers[greasePencilData.activeLayerIndex]
        } else if (greasePencilData.layers.isNotEmpty()) {
            greasePencilData.layers[0]
        } else null
    }

    private fun getActiveFrame(): Frame? {
        return getActiveLayer()?.let { layer ->
            if (layer.frames.isEmpty()) {
                addDefaultFrameToLayer(layer)
            }
            val index = greasePencilData.activeFrameIndex.coerceAtMost(layer.frames.size - 1)
            if (index >= 0) layer.frames[index] else layer.frames[0]
        }
    }

    private fun addDefaultFrameToLayer(layer: Layer) {
        if (layer.frames.isEmpty()) {
            layer.frames.add(Frame(frameNumber = 0))
            greasePencilData.activeFrameIndex = 0
        }
    }

    private fun pickColor(x: Float, y: Float) {
        // Sample color from bitmap at position
        // Implementation would require bitmap readback
    }

    private fun startSelection(x: Float, y: Float) {
        // Selection tool implementation
    }

    private fun startTransform(x: Float, y: Float) {
        // Transform tool implementation
    }

    private fun fillArea(x: Float, y: Float) {
        // Flood fill implementation
    }
}