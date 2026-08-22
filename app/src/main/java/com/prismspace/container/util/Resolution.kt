package com.prismspace.container.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.util.DisplayMetrics
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

object Resolution {
    private const val TAG = "UtilsScreen"

    @JvmStatic
    fun getScreenWidth(context: Context): Int {
        return getScreenSize(context, null).x
    }

    @JvmStatic
    fun getScreenHeight(context: Context): Int {
        return getScreenSize(context, null).y
    }

    @JvmStatic
    @SuppressLint("NewApi")
    fun getScreenSize(context: Context, outSize: Point?): Point {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val ret = outSize ?: Point()

        if (Build.VERSION.SDK_INT >= 30) {
            val windowMetrics = wm.currentWindowMetrics
            val bounds = windowMetrics.bounds
            ret.x = bounds.width()
            ret.y = bounds.height()
        } else if (Build.VERSION.SDK_INT >= 13) {
            @Suppress("DEPRECATION")
            val defaultDisplay = wm.defaultDisplay
            defaultDisplay.getSize(ret)
        } else {
            @Suppress("DEPRECATION")
            val defaultDisplay = wm.defaultDisplay
            ret.x = defaultDisplay.width
            ret.y = defaultDisplay.height
        }
        return ret
    }

    @JvmStatic
    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources: Resources = context.resources
        val metrics: DisplayMetrics = resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }

    @JvmStatic
    fun convertPixelsToDp(px: Float, context: Context): Float {
        val resources: Resources = context.resources
        val metrics: DisplayMetrics = resources.displayMetrics
        return px / (metrics.densityDpi / 160f)
    }

    @JvmStatic
    fun getDensity(context: Context?): Float {
        if (context == null) {
            return 0f
        }
        return try {
            context.resources.displayMetrics.density
        } catch (_: Exception) {
            0f
        }
    }

    @JvmStatic
    fun checkPix(context: Activity, width: Int, height: Int): Boolean {
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            val metrics = DisplayMetrics()
            context.windowManager.defaultDisplay.getRealMetrics(metrics)
            metrics.widthPixels == width && metrics.heightPixels == height
        } else {
            getScreenPixWidth(context) == width && getScreenPixHeight(context) == height
        }
    }

    @JvmStatic
    fun getScreenPixWidth(context: Context): Int {
        return context.resources.displayMetrics.widthPixels
    }

    @JvmStatic
    fun getScreenPixHeight(context: Context): Int {
        return context.resources.displayMetrics.heightPixels
    }

    @JvmStatic
    fun dipToPx(context: Context, dip: Int): Int {
        return (dip * context.resources.displayMetrics.density + 0.5f).toInt()
    }

    @JvmStatic
    fun pxToDip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    @JvmStatic
    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    @JvmStatic
    fun hideInputMethod(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @JvmStatic
    fun showInputMethod(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    @JvmStatic
    fun showInputMethod(view: View, delayMillis: Long) {
        Handler().postDelayed({ showInputMethod(view) }, delayMillis)
    }

    @JvmStatic
    fun isScreenLocked(c: Context): Boolean {
        val keyguardManager = c.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return !keyguardManager.inKeyguardRestrictedInputMode()
    }

    @JvmStatic
    fun getBarHeight(context: Context): Int {
        var sbar = 38
        try {
            val c = Class.forName("com.android.internal.R\$dimen")
            val obj = c.getDeclaredConstructor().newInstance()
            val field = c.getField("status_bar_height")
            val x = field.get(obj)?.toString()?.toIntOrNull() ?: return sbar
            sbar = context.resources.getDimensionPixelSize(x)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sbar
    }

    @JvmStatic
    fun getNavigationBarSize(context: Context): Point {
        val appUsableSize = getScreenSize(context, null)
        val realScreenSize = getRealScreenSize(context)

        if (appUsableSize.x < realScreenSize.x) {
            return Point(realScreenSize.x - appUsableSize.x, appUsableSize.y)
        }

        if (appUsableSize.y < realScreenSize.y) {
            return Point(appUsableSize.x, realScreenSize.y - appUsableSize.y)
        }

        return Point()
    }

    @JvmStatic
    fun getRealScreenSize(context: Context): Point {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val size = Point()

        if (Build.VERSION.SDK_INT >= 30) {
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            size.x = bounds.width()
            size.y = bounds.height()
        } else if (Build.VERSION.SDK_INT >= 17) {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            display.getRealSize(size)
        } else if (Build.VERSION.SDK_INT >= 14) {
            @Suppress("DEPRECATION")
            val display: Display = windowManager.defaultDisplay
            try {
                size.x = Display::class.java.getMethod("getRawWidth").invoke(display) as Int
                size.y = Display::class.java.getMethod("getRawHeight").invoke(display) as Int
            } catch (_: Exception) {
            }
        }

        return size
    }
}

