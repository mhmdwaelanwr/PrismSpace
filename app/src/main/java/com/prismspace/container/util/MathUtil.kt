package com.prismspace.container.util

import android.graphics.Point
import android.graphics.PointF
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object MathUtil {
    @JvmStatic
    fun getDistance(a: PointF, b: PointF): Int {
        return sqrt((a.x - b.x).toDouble().pow(2.0) + (a.y - b.y).toDouble().pow(2.0)).roundToInt()
    }

    @JvmStatic
    fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Int {
        return sqrt((x1 - x2).toDouble().pow(2.0) + (y1 - y2).toDouble().pow(2.0)).roundToInt()
    }

    @JvmStatic
    fun getPointByCutLength(a: Point, b: Point, cutLength: Int): Point {
        val radian = getRadian(a, b)
        return Point(a.x + (cutLength * cos(radian.toDouble())).toInt(), a.y + (cutLength * sin(radian.toDouble())).toInt())
    }

    @JvmStatic
    fun getRadian(a: Point, b: Point): Float {
        val lenA = (b.x - a.x).toFloat()
        val lenB = (b.y - a.y).toFloat()
        val lenC = sqrt((lenA * lenA + lenB * lenB).toDouble()).toFloat()
        var radian = acos((lenA / lenC).toDouble()).toFloat()
        radian *= if (b.y < a.y) -1 else 1
        return radian
    }

    @JvmStatic
    fun angle2Radian(angle: Double): Double {
        return angle / 180 * Math.PI
    }

    @JvmStatic
    fun radian2Angle(radian: Double): Double {
        return radian / Math.PI * 180
    }
}

