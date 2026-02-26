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
    private var activeRecorder: AudioRecord? = null
    @Volatile
    private var isRecordingRequested = false

    /**
     * Starts continuous recording. Audio data is delivered via the [onDataReceived] callback.
     * [onDataReceived] is called with null when recording has fully stopped.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startRecording(
        scope: CoroutineScope,
        sampleRate: Int = 16000,
        fmt: Int = AudioFormat.ENCODING_PCM_16BIT,
        onDataReceived: (ByteArray?) -> Unit
    ) {
        if (isRecordingRequested || recordingJob?.isActive == true) {
            Log.w(TAG, "Recording is already in progress or stopping, ignoring start request")
            return
        }
        isRecordingRequested = true
        
        recordingJob = scope.launch(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    fmt
                ) * 2
                
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    fmt,
                    bufferSize
                )
                activeRecorder = recorder

                val buffer = ByteArray(bufferSize)
                recorder.startRecording()
                Log.d(TAG, "Recording started")
                
                while (isActive && isRecordingRequested) {
                    val readResult = recorder.read(buffer, 0, bufferSize)
                    if (readResult > 0 && isActive && isRecordingRequested) {
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
                Log.d(TAG, "Cleaning up recording resources...")
                isRecordingRequested = false
                try {
                    recorder?.stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recorder", e)
                }
                recorder?.release()
                if (activeRecorder == recorder) activeRecorder = null
                
                // Signal end to callback
                onDataReceived(null)
                
                Log.d(TAG, "Recording stopped and resources released")
            }
        }
    }

    /**
     * Stops the current recording session.
     */
    fun stopRecording() {
        Log.d(TAG, "stopRecording called")
        isRecordingRequested = false
        recordingJob?.cancel()
        try {
            activeRecorder?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling stop() on activeRecorder", e)
        }
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
            while (isActive && audioTrack.playbackHeadPosition < (buffer.size / 2)) { // Adjusted for 16-bit PCM (2 bytes per sample)
                delay(10)
            }
            // Give it a tiny bit more time to flush the buffer
            delay(50)
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
