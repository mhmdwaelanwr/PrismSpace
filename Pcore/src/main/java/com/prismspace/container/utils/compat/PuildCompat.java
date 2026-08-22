package com.prismspace.container.utils.compat;

import android.os.Build;

/**
 * Android platform/ROM capability helpers.
 *
 * Keep platform checks centralized here. New engine code should prefer capability probing where
 * possible, but when an API-level gate is unavoidable these helpers provide one consistent source
 * of truth (including preview builds).
 */
public class PuildCompat {

    public static int getPreviewSDKInt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                return Build.VERSION.PREVIEW_SDK_INT;
            } catch (Throwable ignored) {
            }
        }
        return 0;
    }

    /** Visible for unit tests so version semantics can be validated without mutating Build.VERSION. */
    static boolean isAtLeast(int sdkInt, int previewSdkInt, int targetApi) {
        if (sdkInt >= targetApi) {
            return true;
        }
        return sdkInt == targetApi - 1 && previewSdkInt > 0;
    }

    public static boolean isAndroid17() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 37);
    }

    public static boolean isAndroid16() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 36);
    }

    public static boolean isAndroid15() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 35);
    }

    /** Android 14 / UpsideDownCake (API 34). */
    public static boolean isU() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 34);
    }

    /** Android 13 / Tiramisu (API 33). */
    public static boolean isTiramisu() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 33);
    }

    public static boolean isS() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 31);
    }

    public static boolean isR() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 30);
    }

    public static boolean isQ() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 29);
    }

    public static boolean isPie() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 28);
    }

    public static boolean isOreo() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 26);
    }

    public static boolean isN_MR1() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 25);
    }

    public static boolean isN() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 24);
    }

    public static boolean isM() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 23);
    }

    public static boolean isL() {
        return isAtLeast(Build.VERSION.SDK_INT, getPreviewSDKInt(), 21);
    }

    public static boolean isSamsung() {
        return "samsung".equalsIgnoreCase(Build.BRAND) || "samsung".equalsIgnoreCase(Build.MANUFACTURER);
    }

    public static boolean isEMUI() {
        if (Build.DISPLAY.toUpperCase().startsWith("EMUI")) {
            return true;
        }
        String property = SystemPropertiesCompat.get("ro.build.version.emui");
        return property != null && property.contains("EmotionUI");
    }

    public static boolean isMIUI() {
        return SystemPropertiesCompat.getInt("ro.miui.ui.version.code", 0) > 0;
    }

    public static boolean isFlyme() {
        return Build.DISPLAY.toLowerCase().contains("flyme");
    }

    public static boolean isColorOS() {
        return SystemPropertiesCompat.isExist("ro.build.version.opporom")
                || SystemPropertiesCompat.isExist("ro.rom.different.version");
    }

    public static boolean is360UI() {
        String property = SystemPropertiesCompat.get("ro.build.uiversion");
        return property != null && property.toUpperCase().contains("360UI");
    }

    public static boolean isLetv() {
        return Build.MANUFACTURER.equalsIgnoreCase("Letv");
    }

    public static boolean isVivo() {
        return SystemPropertiesCompat.isExist("ro.vivo.os.build.display.id");
    }

    private static ROMType sRomType;

    public static ROMType getROMType() {
        if (sRomType == null) {
            if (isEMUI()) {
                sRomType = ROMType.EMUI;
            } else if (isMIUI()) {
                sRomType = ROMType.MIUI;
            } else if (isFlyme()) {
                sRomType = ROMType.FLYME;
            } else if (isColorOS()) {
                sRomType = ROMType.COLOR_OS;
            } else if (is360UI()) {
                sRomType = ROMType._360;
            } else if (isLetv()) {
                sRomType = ROMType.LETV;
            } else if (isVivo()) {
                sRomType = ROMType.VIVO;
            } else if (isSamsung()) {
                sRomType = ROMType.SAMSUNG;
            } else {
                sRomType = ROMType.OTHER;
            }
        }
        return sRomType;
    }

    public enum ROMType {
        EMUI,
        MIUI,
        FLYME,
        COLOR_OS,
        LETV,
        VIVO,
        _360,
        SAMSUNG,
        OTHER
    }
}
