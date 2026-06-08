package com.example.chess.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object ChessSoundPlayer {
    var isSoundEnabled: Boolean = true

    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("ChessSoundPlayer", "Caught uncaught sound-playing coroutine exception", throwable)
    })

    @Volatile
    private var soundPool: SoundPool? = null

    @Volatile
    private var moveSoundId = 0
    @Volatile
    private var captureSoundId = 0
    @Volatile
    private var checkSoundId = 0
    @Volatile
    private var checkmateSoundId = 0

    @Volatile
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        val appContext = context
        scope.launch {
            synchronized(this@ChessSoundPlayer) {
                if (isInitialized) return@launch
                try {
                    if (soundPool == null) {
                        soundPool = SoundPool.Builder()
                            .setMaxStreams(5)
                            .setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_MEDIA)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            .build()
                    }

                    val cacheDir = appContext.cacheDir ?: return@launch

                    val moveFile = File(cacheDir, "chess_move.wav")
                    if (!moveFile.exists() || moveFile.length() == 0L) {
                        val buf = generateMoveBuffer()
                        saveWavFile(moveFile, buf)
                    }
                    if (moveSoundId == 0) {
                        moveSoundId = soundPool?.load(moveFile.absolutePath, 1) ?: 0
                    }

                    val captureFile = File(cacheDir, "chess_capture.wav")
                    if (!captureFile.exists() || captureFile.length() == 0L) {
                        val buf = generateCaptureBuffer()
                        saveWavFile(captureFile, buf)
                    }
                    if (captureSoundId == 0) {
                        captureSoundId = soundPool?.load(captureFile.absolutePath, 1) ?: 0
                    }

                    val checkFile = File(cacheDir, "chess_check.wav")
                    if (!checkFile.exists() || checkFile.length() == 0L) {
                        val buf = generateCheckBuffer()
                        saveWavFile(checkFile, buf)
                    }
                    if (checkSoundId == 0) {
                        checkSoundId = soundPool?.load(checkFile.absolutePath, 1) ?: 0
                    }

                    val checkmateFile = File(cacheDir, "chess_checkmate.wav")
                    if (!checkmateFile.exists() || checkmateFile.length() == 0L) {
                        val buf = generateCheckmateBuffer()
                        saveWavFile(checkmateFile, buf)
                    }
                    if (checkmateSoundId == 0) {
                        checkmateSoundId = soundPool?.load(checkmateFile.absolutePath, 1) ?: 0
                    }

                    isInitialized = true
                    android.util.Log.d("ChessSoundPlayer", "ChessSoundPlayer using SoundPool synchronized and initialized successfully.")
                } catch (t: Throwable) {
                    android.util.Log.e("ChessSoundPlayer", "Failed pre-synthesizing or loading chess sounds", t)
                }
            }
        }
    }

    private fun saveWavFile(file: File, data: ShortArray) {
        val parent = file.parentFile ?: return
        val tempFile = File(parent, "${file.name}.tmp")
        val totalAudioLen = data.size * 2
        val totalDataLen = totalAudioLen + 36

        try {
            FileOutputStream(tempFile).use { fos ->
                fos.write("RIFF".toByteArray())
                fos.write(intToBytes(totalDataLen))
                fos.write("WAVE".toByteArray())
                
                fos.write("fmt ".toByteArray())
                fos.write(intToBytes(16))
                fos.write(shortToBytes(1))
                fos.write(shortToBytes(1))
                fos.write(intToBytes(SAMPLE_RATE))
                fos.write(intToBytes(SAMPLE_RATE * 2))
                fos.write(shortToBytes(2))
                fos.write(shortToBytes(16))
                
                fos.write("data".toByteArray())
                fos.write(intToBytes(totalAudioLen))
                
                val pcmBuffer = ByteBuffer.allocate(data.size * 2).order(ByteOrder.LITTLE_ENDIAN)
                for (sample in data) {
                    pcmBuffer.putShort(sample)
                }
                fos.write(pcmBuffer.array())
                fos.flush()
            }
            if (!tempFile.renameTo(file)) {
                if (file.exists()) {
                    file.delete()
                }
                tempFile.renameTo(file)
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
            throw e
        }
    }

    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToBytes(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }

    private fun play(soundId: Int) {
        if (!isSoundEnabled || soundId == 0) return
        try {
            soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } catch (t: Throwable) {
            android.util.Log.e("ChessSoundPlayer", "Failed to play sound ID $soundId", t)
        }
    }

    fun playMove() {
        play(moveSoundId)
    }

    fun playCapture() {
        play(captureSoundId)
    }

    fun playCheck() {
        play(checkSoundId)
    }

    fun playCheckmate() {
        play(checkmateSoundId)
    }

    private fun generateMoveBuffer(): ShortArray {
        val durationMs = 120
        val size = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(size)

        for (i in 0 until size) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // Short wooden striking transient noise
            val noiseDecay = Math.exp(-t * 1200.0)
            val noise = (Math.random() * 2.0 - 1.0) * 0.25 * noiseDecay

            // Modal resonance of wooden piece (three fixed non-gliding harmonics)
            val s1 = sin(2.0 * Math.PI * 300.0 * t)
            val s2 = sin(2.0 * Math.PI * 460.0 * t) * 0.40
            val s3 = sin(2.0 * Math.PI * 680.0 * t) * 0.15
            val resonance = (s1 + s2 + s3) / 1.55

            // Sharp physical decay envelope mimicking wood board absorption
            val envelope = Math.exp(-t * 55.0)

            val sampleValue = (resonance * 0.75 + noise * 0.25) * envelope
            buffer[i] = (sampleValue * 18000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateCaptureBuffer(): ShortArray {
        val durationMs = 180
        val size = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(size)

        for (i in 0 until size) {
            val t = i.toDouble() / SAMPLE_RATE

            // --- Impact 1 (Collision of Pieces) at t = 0 ---
            val t1 = t
            val env1 = Math.exp(-t1 * 110.0)
            val noiseDecay1 = Math.exp(-t1 * 1500.0)
            val noise1 = (Math.random() * 2.0 - 1.0) * 0.35 * noiseDecay1
            val s1_1 = sin(2.0 * Math.PI * 420.0 * t1)
            val s1_2 = sin(2.0 * Math.PI * 650.0 * t1) * 0.45
            val s1_3 = sin(2.0 * Math.PI * 950.0 * t1) * 0.25
            val res1 = (s1_1 + s1_2 + s1_3) / 1.70
            val p1 = (res1 * 0.70 + noise1 * 0.30) * env1

            // --- Impact 2 (Board Landing) delayed by 55 ms ---
            val delaySeconds = 0.055
            val p2 = if (t >= delaySeconds) {
                val t2 = t - delaySeconds
                val env2 = Math.exp(-t2 * 50.0)
                val noiseDecay2 = Math.exp(-t2 * 1000.0)
                val noise2 = (Math.random() * 2.0 - 1.0) * 0.20 * noiseDecay2
                val s2_1 = sin(2.0 * Math.PI * 290.0 * t2)
                val s2_2 = sin(2.0 * Math.PI * 440.0 * t2) * 0.40
                val s2_3 = sin(2.0 * Math.PI * 650.0 * t2) * 0.15
                val res2 = (s2_1 + s2_2 + s2_3) / 1.55
                (res2 * 0.75 + noise2 * 0.25) * env2
            } else {
                0.0
            }

            val sampleValue = p1 + p2
            buffer[i] = (sampleValue * 18000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateCheckBuffer(): ShortArray {
        val durationMs = 250
        val size = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(size)

        for (i in 0 until size) {
            val t = i.toDouble() / SAMPLE_RATE

            // First alert strike
            val t1 = t
            val env1 = Math.exp(-t1 * 35.0)
            val s1_1 = sin(2.0 * Math.PI * 523.25 * t1) // C5
            val s1_2 = sin(2.0 * Math.PI * 783.99 * t1) * 0.5 // G5
            val strike1 = (s1_1 + s1_2) * 0.6 * env1

            // Second alert strike (delayed by 80 ms, slightly higher pitch for rising alert feel)
            val delaySeconds = 0.080
            val strike2 = if (t >= delaySeconds) {
                val t2 = t - delaySeconds
                val env2 = Math.exp(-t2 * 30.0)
                val s2_1 = sin(2.0 * Math.PI * 587.33 * t2) // D5
                val s2_2 = sin(2.0 * Math.PI * 880.00 * t2) * 0.5 // A5
                (s2_1 + s2_2) * 0.6 * env2
            } else {
                0.0
            }

            // Combine strikes with a subtle high alert ring
            val ringEnv = Math.exp(-t * 15.0)
            val highRing = sin(2.0 * Math.PI * 1046.50 * t) * 0.08 * ringEnv // C6 high ring

            val sampleValue = strike1 + strike2 + highRing
            buffer[i] = (sampleValue * 15000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }

    private fun generateCheckmateBuffer(): ShortArray {
        val durationMs = 600
        val size = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(size)

        // Cascade of 4 notes: C5 (523.25), E5 (659.25), G5 (783.99), C6 (1046.50)
        val notes = listOf(523.25, 659.25, 783.99, 1046.50)
        val delays = listOf(0.0, 0.08, 0.16, 0.24)

        for (i in 0 until size) {
            val t = i.toDouble() / SAMPLE_RATE
            var sampleValue = 0.0

            for (n in notes.indices) {
                val delay = delays[n]
                if (t >= delay) {
                    val tn = t - delay
                    // Each wood chime has standard base resonance and its harmonics
                    val env = Math.exp(-tn * 15.0) // slightly longer decay for woodblock chime feel
                    val s1 = sin(2.0 * Math.PI * notes[n] * tn)
                    val s2 = sin(2.0 * Math.PI * notes[n] * 1.5 * tn) * 0.35
                    val s3 = sin(2.0 * Math.PI * notes[n] * 2.2 * tn) * 0.15
                    val noteVal = (s1 + s2 + s3) / 1.5 * env
                    sampleValue += noteVal * 0.35
                }
            }

            // Subtle base low wood impact at the very end to anchor the checkmate
            val subDelay = 0.32
            val subValue = if (t >= subDelay) {
                val ts = t - subDelay
                val envSub = Math.exp(-ts * 10.0)
                sin(2.0 * Math.PI * 150.0 * ts) * 0.20 * envSub
            } else {
                0.0
            }

            buffer[i] = ((sampleValue + subValue) * 16000).toInt().coerceIn(-32768, 32767).toShort()
        }
        return buffer
    }
}
