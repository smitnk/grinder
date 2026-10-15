package com.smitnk.grinder.data

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Stroke Point - equivalent to Blender's bGPDspoint
 * Represents a single point in a stroke with position, pressure, and time
 */
@Serializable
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f,
    val strength: Float = 1.0f,
    val time: Long = System.currentTimeMillis(),
    val uvRotation: Float = 0.0f
) : Parcelable {
    constructor(parcel: Parcel) : this(
        x = parcel.readFloat(),
        y = parcel.readFloat(),
        pressure = parcel.readFloat(),
        strength = parcel.readFloat(),
        time = parcel.readLong(),
        uvRotation = parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeFloat(pressure)
        parcel.writeFloat(strength)
        parcel.writeLong(time)
        parcel.writeFloat(uvRotation)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<StrokePoint> {
        override fun createFromParcel(parcel: Parcel): StrokePoint = StrokePoint(parcel)
        override fun newArray(size: Int): Array<StrokePoint?> = arrayOfNulls(size)
    }
}

/**
 * Stroke - equivalent to Blender's bGPDstroke
 * A collection of points forming a single stroke
 */
@Serializable
data class Stroke(
    val id: String = UUID.randomUUID().toString(),
    val points: MutableList<StrokePoint> = mutableListOf(),
    val color: Int = 0xFF000000,  // ARGB
    val thickness: Float = 4.0f,
    val hardness: Float = 1.0f,
    val opacity: Float = 1.0f,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val capStyle: CapStyle = CapStyle.ROUND,
    val joinStyle: JoinStyle = JoinStyle.ROUND,
    val select: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = System.currentTimeMillis()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: UUID.randomUUID().toString(),
        points = parcel.createTypedArrayList(StrokePoint.CREATOR)?.toMutableList() ?: mutableListOf(),
        color = parcel.readInt(),
        thickness = parcel.readFloat(),
        hardness = parcel.readFloat(),
        opacity = parcel.readFloat(),
        blendMode = BlendMode.values()[parcel.readInt()],
        capStyle = CapStyle.values()[parcel.readInt()],
        joinStyle = JoinStyle.values()[parcel.readInt()],
        select = parcel.readInt() == 1,
        startTime = parcel.readLong(),
        endTime = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeTypedList(points)
        parcel.writeInt(color)
        parcel.writeFloat(thickness)
        parcel.writeFloat(hardness)
        parcel.writeFloat(opacity)
        parcel.writeInt(blendMode.ordinal)
        parcel.writeInt(capStyle.ordinal)
        parcel.writeInt(joinStyle.ordinal)
        parcel.writeInt(if (select) 1 else 0)
        parcel.writeLong(startTime)
        parcel.writeLong(endTime)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Stroke> {
        override fun createFromParcel(parcel: Parcel): Stroke = Stroke(parcel)
        override fun newArray(size: Int): Array<Stroke?> = arrayOfNulls(size)
    }
}

/**
 * Frame - equivalent to Blender's bGPDframe
 * A frame containing strokes at a specific time/index
 */
@Serializable
data class Frame(
    val id: String = UUID.randomUUID().toString(),
    val frameNumber: Int = 0,
    val strokes: MutableList<Stroke> = mutableListOf(),
    val select: Boolean = false
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: UUID.randomUUID().toString(),
        frameNumber = parcel.readInt(),
        strokes = parcel.createTypedArrayList(Stroke.CREATOR)?.toMutableList() ?: mutableListOf(),
        select = parcel.readInt() == 1
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeInt(frameNumber)
        parcel.writeTypedList(strokes)
        parcel.writeInt(if (select) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Frame> {
        override fun createFromParcel(parcel: Parcel): Frame = Frame(parcel)
        override fun newArray(size: Int): Array<Frame?> = arrayOfNulls(size)
    }
}

/**
 * Layer - equivalent to Blender's bGPDlayer
 * Contains frames and layer properties
 */
@Serializable
data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer",
    val frames: MutableList<Frame> = mutableListOf(),
    val opacity: Float = 1.0f,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val select: Boolean = false,
    val tintColor: Int = 0x00000000,
    val blendMode: BlendMode = BlendMode.NORMAL,
    val onionSkinning: Boolean = false,
    val onionSkinForward: Int = 2,
    val onionSkinBackward: Int = 2
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: UUID.randomUUID().toString(),
        name = parcel.readString() ?: "Layer",
        frames = parcel.createTypedArrayList(Frame.CREATOR)?.toMutableList() ?: mutableListOf(),
        opacity = parcel.readFloat(),
        visible = parcel.readInt() == 1,
        locked = parcel.readInt() == 1,
        select = parcel.readInt() == 1,
        tintColor = parcel.readInt(),
        blendMode = BlendMode.values()[parcel.readInt()],
        onionSkinning = parcel.readInt() == 1,
        onionSkinForward = parcel.readInt(),
        onionSkinBackward = parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeTypedList(frames)
        parcel.writeFloat(opacity)
        parcel.writeInt(if (visible) 1 else 0)
        parcel.writeInt(if (locked) 1 else 0)
        parcel.writeInt(if (select) 1 else 0)
        parcel.writeInt(tintColor)
        parcel.writeInt(blendMode.ordinal)
        parcel.writeInt(if (onionSkinning) 1 else 0)
        parcel.writeInt(onionSkinForward)
        parcel.writeInt(onionSkinBackward)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Layer> {
        override fun createFromParcel(parcel: Parcel): Layer = Layer(parcel)
        override fun newArray(size: Int): Array<Layer?> = arrayOfNulls(size)
    }
}

/**
 * Grease Pencil Data - equivalent to Blender's bGPdata
 * Root container for all layers
 */
@Serializable
data class GreasePencilData(
    val id: String = UUID.randomUUID().toString(),
    val layers: MutableList<Layer> = mutableListOf(),
    val activeLayerIndex: Int = -1,
    val activeFrameIndex: Int = -1,
    val sourceType: SourceType = SourceType.NONE
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: UUID.randomUUID().toString(),
        layers = parcel.createTypedArrayList(Layer.CREATOR)?.toMutableList() ?: mutableListOf(),
        activeLayerIndex = parcel.readInt(),
        activeFrameIndex = parcel.readInt(),
        sourceType = SourceType.values()[parcel.readInt()]
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeTypedList(layers)
        parcel.writeInt(activeLayerIndex)
        parcel.writeInt(activeFrameIndex)
        parcel.writeInt(sourceType.ordinal)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<GreasePencilData> {
        override fun createFromParcel(parcel: Parcel): GreasePencilData = GreasePencilData(parcel)
        override fun newArray(size: Int): Array<GreasePencilData?> = arrayOfNulls(size)
    }
}

enum class BlendMode {
    NORMAL, ADD, SUBTRACT, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN,
    COLOR_DODGE, COLOR_BURN, HARD_LIGHT, SOFT_LIGHT, DIFFERENCE, EXCLUSION
}

enum class CapStyle {
    ROUND, SQUARE, BUTT
}

enum class JoinStyle {
    ROUND, BEVEL, MITER
}

enum class SourceType {
    NONE, SCENE, OBJECT, CURVES, MASK, ANNOTATION
}