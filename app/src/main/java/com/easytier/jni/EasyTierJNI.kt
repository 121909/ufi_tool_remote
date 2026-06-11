package com.easytier.jni

object EasyTierJNI {
    init {
        runCatching { System.loadLibrary("easytier_ffi") }
        System.loadLibrary("easytier_android_jni")
    }

    @JvmStatic external fun parseConfig(config: String): Int
    @JvmStatic external fun runNetworkInstance(config: String): Int
    @JvmStatic external fun retainNetworkInstance(instanceNames: Array<String>?): Int
    @JvmStatic external fun collectNetworkInfos(maxLength: Int): String?
    @JvmStatic external fun listInstances(maxLength: Int): String?
    @JvmStatic external fun getLastError(): String?

    @JvmStatic
    fun stopAllInstances(): Int = retainNetworkInstance(null)
}
