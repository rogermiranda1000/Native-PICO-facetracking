package com.pico.engine.ft_sdk;

/**
 * Adapter to the C/C++ code related with face-tracking.
 * All the functions are found inside `libPxrPlatform.so`. You can also find the definition on `PXR_Plugin.cs`, in the SDK.
 */
public class FaceTracking {

    /**
     * PxrPlatform call to enable eye-tracking
     * @param enable Enable eye-tracking (true) or disable it (false)
     */
    public static native void Pxr_EnableEyeTracking(boolean enable);

    static {
        System.loadLibrary("PxrPlatform"); // include PXR_Plugin's PXR_PLATFORM_DLL
    }
}
