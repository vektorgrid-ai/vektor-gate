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
import kotlinx.serialization.Serializable
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

object RelayWebsocketClient {
    enum class RelayStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        READY,
        STREAMING_AUDIO,
        PROCESSING,
        PLAYING_TTS
    }
    private class RelayWebsocketListener(val client: RelayWebsocketClient) : WebSocketListener() {
        companion object {
            private const val TAG = "RelayWebsocketListener"
        }

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
            val doc = Json.parseToJsonElement(text)
            val type = doc.jsonObject["type"]?.jsonPrimitive?.content
            when (type) {
                "hello.ack" -> {
                    Log.d(TAG, "Hello message acknowledged")
                    client.setStatus(RelayStatus.READY)
                }
                "session.ack" -> {
                    Log.d(TAG, "Session start acknowledged")
                    client.setSessionId(doc.jsonObject["session_id"]?.jsonPrimitive?.content)
                    client.setStatus(RelayStatus.STREAMING_AUDIO)
                }
                "tts.start" -> {
                    Log.d(TAG, "TTS playback started")
                    client.setStatus(RelayStatus.PLAYING_TTS)
                }
                "tts.end" -> {
                    Log.d(TAG, "TTS playback ended")
                    client.playCollectedTts()
                    client.setStatus(RelayStatus.READY)
                }
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

    private var socket: WebSocket? = null
    private var audioManager: AudioManager? = null
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
        val client = OkHttpClient()
        socket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    fun disconnect() {
        socket?.close(1000, null)
        _status.value = RelayStatus.DISCONNECTED
        sessionId = null
        socket = null
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startSession() {
        sendSessionStartMessage()
        audioManager = AudioManager()

        val scope = CoroutineScope(Dispatchers.IO)
        audioManager?.startRecording(scope, onDataReceived = { data ->
            sendAudioBytes(data)
        })
    }

    fun endSession() {
        audioManager?.stopRecording()
        sendAudioEndMessage()
        _status.value = RelayStatus.PROCESSING
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

    private fun sendSessionStartMessage() {
        val message = SessionStartMessage(
            type = "session.start",
            timestamp = System.currentTimeMillis() / 1000,
            session_id = "testing-session"
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
            session_id = sessionId ?: "testing-session",
            reason = "user_ended"
        )

        val json = Json.encodeToString(AudioEndMessage.serializer(), message)
        socket?.send(json)
    }

    private fun playCollectedTts() {
        val manager = AudioManager()
        val dataToPlay = ttsBuffer
        ttsBuffer = ByteArray(0)
        CoroutineScope(Dispatchers.IO).launch {
            manager.playAudio(dataToPlay)
        }
    }

    private fun setStatus(status: RelayStatus) {
        _status.value = status
    }

    private fun setSessionId(id: String?) {
        sessionId = id
    }

    private fun addTtsData(data: ByteArray) {
        ttsBuffer += data
    }
}
