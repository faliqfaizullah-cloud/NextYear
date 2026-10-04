package com.nextyear.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object Haptics {
    private var vib: Vibrator? = null

    @Suppress("DEPRECATION")
    fun init(c: Context) {
        vib = if (Build.VERSION.SDK_INT >= 31) {
            (c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Suppress("DEPRECATION")
    fun buzz(ms: Long, amp: Int = -1) {
        val v = vib ?: return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, amp))
        else v.vibrate(ms)
    }

    fun tick() = buzz(10, 70)
    fun soft() = buzz(18, 120)
    fun click() = buzz(28, 190)
    fun heavy() = buzz(45, 255)
}
