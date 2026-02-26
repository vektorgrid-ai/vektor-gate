package com.example.vektorgate.relay

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.vektorgate.relay.audio.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.UUID

object RelayWebsocketClient {
    private const val TAG = "RelayWebsocketClient"

    enum class RelayStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
        STARTING_SESSION,
        STREAMING_AUDIO,
        PROCESSING,
        PLAYING_TTS
    }

    private class RelayWebsocketListener(val client: RelayWebsocketClient) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Socket opened")
            client.setStatus(RelayStatus.CONNECTED)
            client.sendHelloMessage()
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            client.addTtsData(bytes.toByteArray())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Socket message received: $text")
            try {
                val doc = Json.parseToJsonElement(text)
                val type = doc.jsonObject["type"]?.jsonPrimitive?.content
                when (type) {
                    "hello.ack" -> {
                        Log.d(TAG, "Hello message acknowledged")
                        client.setStatus(RelayStatus.READY)
                    }
                    "session.ack" -> {
                        Log.d(TAG, "Session start acknowledged")
                        val serverSessionId = doc.jsonObject["session_id"]?.jsonPrimitive?.content
                        client.setSessionId(serverSessionId)
                        client.setStatus(RelayStatus.STREAMING_AUDIO)
                    }
                    "tts.start" -> {
                        Log.d(TAG, "TTS playback started")
                        client.stopRecordingOnly()
                        client.setStatus(RelayStatus.PLAYING_TTS)
                    }
                    "tts.end" -> {
                        Log.d(TAG, "TTS playback ended")
                        client.playCollectedTts()
                    }
                    "error" -> {
                        val message = doc.jsonObject["message"]?.jsonPrimitive?.content
                        Log.e(TAG, "Relay Error: $message")
                        client.setStatus(RelayStatus.READY)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing message", e)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "Socket closing with code $code and reason $reason")
            client.setStatus(RelayStatus.DISCONNECTED)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "Socket failure: ${t.message}")
            client.setStatus(RelayStatus.DISCONNECTED)
        }
    }

    private val httpClient = OkHttpClient()
    private var socket: WebSocket? = null
    private val audioManager = AudioManager()
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private val _status = MutableStateFlow(RelayStatus.DISCONNECTED)
    val status: StateFlow<RelayStatus> = _status.asStateFlow()
    
    private var sessionId: String? = null
    private var ttsBuffer: ByteArray = ByteArray(0)

    fun connect(url: String) {
        if (_status.value != RelayStatus.DISCONNECTED) return
        _status.value = RelayStatus.CONNECTING
        
        val request = Request.Builder()
            .url(url)
            .build()

        val listener = RelayWebsocketListener(this)
        socket = httpClient.newWebSocket(request, listener)
    }

    fun disconnect() {
        socket?.close(1000, "User disconnected")
        audioManager.stopRecording()
        _status.value = RelayStatus.DISCONNECTED
        sessionId = null
        socket = null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startSession() {
        if (_status.value != RelayStatus.READY) {
            Log.w(TAG, "Cannot start session: status is ${_status.value}")
            return
        }

        audioManager.stopRecording()
        ttsBuffer = ByteArray(0)

        val newSessionId = "session-${UUID.randomUUID()}"
        sessionId = newSessionId
        Log.d(TAG, "Starting new session: $newSessionId")

        _status.value = RelayStatus.STARTING_SESSION
        
        sendSessionStartMessage(newSessionId)
        
        // 5. Start recording audio
        audioManager.startRecording(clientScope, onDataReceived = { data ->
            // TODO: wait until session acknowledged (STREAMING_AUDIO)
            if (data == null) {
                Log.d(TAG, "Recording stream closed (null received)")
                return@startRecording
            }
            
            // ONLY send audio when we are in the STREAMING_AUDIO state.
            // This prevents "pre-roll" audio from triggering VAD too early 
            // and ensures we stop sending immediately when session ends.
            if (_status.value == RelayStatus.STREAMING_AUDIO) {
                sendAudioBytes(data)
            }
        })
    }

    fun endSession() {
        Log.d(TAG, "Ending session $sessionId...")
        // Moving to PROCESSING immediately stops the audio flow in the startRecording callback
        _status.value = RelayStatus.PROCESSING
        
        audioManager.stopRecording()
        sendAudioEndMessage()
    }

    internal fun stopRecordingOnly() {
        audioManager.stopRecording()
    }

    private fun sendHelloMessage() {
        val message = HelloMessage(
            type = "hello",
            protocol_version = 1,
            satellite_id = "satellite-id",
            area = "area",
            language = "language",
            capabilities = RelayCapabilities(
                speaker = true,
                display = true,
                supports_barge_in = true,
                supports_streaming_tts = false
            ),
            audio_format = RelayAudioFormat(
                encoding = "pcm_s16le",
                sample_rate = 16000,
                channels = 1,
                frame_ms = 20
            )
        )

        val json = Json.encodeToString(HelloMessage.serializer(), message)
        socket?.send(json)
    }

    private fun sendSessionStartMessage(id: String) {
        val message = SessionStartMessage(
            type = "session.start",
            timestamp = System.currentTimeMillis() / 1000,
            session_id = id
        )

        val json = Json.encodeToString(SessionStartMessage.serializer(), message)
        socket?.send(json)
    }

    private fun sendAudioBytes(data: ByteArray) {
        socket?.send(data.toByteString())
    }

    private fun sendAudioEndMessage() {
        val message = AudioEndMessage(
            type = "audio.end",
            session_id = sessionId ?: "unknown",
            reason = "user_ended"
        )

        val json = Json.encodeToString(AudioEndMessage.serializer(), message)
        socket?.send(json)
    }

    private fun playCollectedTts() {
        if (ttsBuffer.isEmpty()) return
        Log.d(TAG, "Playing collected TTS data (${ttsBuffer.size} bytes)")
        val playbackManager = AudioManager()
        val dataToPlay = ttsBuffer
        ttsBuffer = ByteArray(0)
        clientScope.launch {
            playbackManager.playAudio(dataToPlay)
            _status.value = RelayStatus.READY
        }
    }

    private fun setStatus(status: RelayStatus) {
        Log.d(TAG, "Status changing: ${_status.value} -> $status")
        _status.value = status
    }

    private fun setSessionId(id: String?) {
        if (id != null) {
            sessionId = id
        }
    }

    private fun addTtsData(data: ByteArray) {
        ttsBuffer += data
    }
}
