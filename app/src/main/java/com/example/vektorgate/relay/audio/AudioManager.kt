package com.example.vektorgate.relay.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AudioManager {
    companion object {
        private const val TAG = "AudioManager"

        suspend fun ensureRecordingPermissionGranted(activity: ComponentActivity): Boolean = suspendCancellableCoroutine { continuation ->
            val granted = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
            if (granted == PackageManager.PERMISSION_GRANTED) {
                continuation.resume(true)
            } else {
                val key = "AudioManager_RecordAudio"
                val registry = activity.activityResultRegistry
                var launcher: ActivityResultLauncher<String>? = null
                launcher = registry.register(key, ActivityResultContracts.RequestPermission()) { isGranted ->
                    launcher?.unregister()
                    if (continuation.isActive) {
                        continuation.resume(isGranted)
                    }
                }
                launcher.launch(Manifest.permission.RECORD_AUDIO)
                continuation.invokeOnCancellation {
                    launcher.unregister()
                }
            }
        }
    }
    
    private var recordingJob: Job? = null
    private var isRecording = false

    /**
     * Starts continuous recording. Audio data is delivered via the [onDataReceived] callback.
     * This method is non-blocking and manages its own coroutine on [scope].
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        scope: CoroutineScope,
        sampleRate: Int = 16000,
        fmt: Int = AudioFormat.ENCODING_PCM_16BIT,
        onDataReceived: (ByteArray) -> Unit
    ) {
        if (isRecording) return
        isRecording = true
        
        recordingJob = scope.launch(Dispatchers.IO) {
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                fmt
            ) * 2
            
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                fmt,
                bufferSize
            )

            val buffer = ByteArray(bufferSize)
            try {
                recorder.startRecording()
                Log.d(TAG, "Recording started")
                
                while (isActive && isRecording) {
                    val readResult = recorder.read(buffer, 0, bufferSize)
                    if (readResult > 0) {
                        // Deliver a copy of the recorded data
                        onDataReceived(buffer.copyOf(readResult))
                    } else if (readResult < 0) {
                        Log.e(TAG, "Error reading audio data: $readResult")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during recording", e)
            } finally {
                recorder.stop()
                recorder.release()
                isRecording = false
                Log.d(TAG, "Recording stopped")
            }
        }
    }

    /**
     * Stops the current recording session.
     */
    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
    }

    /**
     * Plays the provided PCM buffer.
     * NOTE: This function waits for playback to finish before returning.
     */
    suspend fun playAudio(
        buffer: ByteArray,
        sampleRate: Int = 16000, 
        fmt: Int = AudioFormat.ENCODING_PCM_16BIT
    ) = withContext(Dispatchers.IO) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            fmt
        )
        
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(fmt)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, buffer.size * 2))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        
        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Wait until the audio is actually finished playing
            while (isActive && audioTrack.playbackHeadPosition < buffer.size) {
                delay(10)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
        } finally {
            try {
                audioTrack.stop()
            } catch (_: Exception) { /* Ignore */ }
            audioTrack.release()
        }
    }
}
