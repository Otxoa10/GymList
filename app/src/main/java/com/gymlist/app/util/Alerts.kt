package com.gymlist.app.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Pitido + vibración al acabar el descanso: en el gimnasio el móvil está en el banco, no en la
 * mano, así que el aviso tiene que oírse o notarse sin mirar la pantalla.
 *
 * Todo va envuelto en runCatching porque un móvil sin motor de vibración, o con el canal de
 * audio ocupado, no debe tumbar el entrenamiento.
 */
fun Context.playRestFinishedAlert() {
    runCatching {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 220, 130, 220), -1)
            )
        }
    }

    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
        // ToneGenerator hay que liberarlo a mano; si se suelta antes, el pitido se corta.
        Handler(Looper.getMainLooper()).postDelayed({ runCatching { tone.release() } }, 900)
    }
}
