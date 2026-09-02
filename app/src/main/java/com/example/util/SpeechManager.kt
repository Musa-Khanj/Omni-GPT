package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<String?>(null)
    val currentSpeakingId: StateFlow<String?> = _currentSpeakingId.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        _currentSpeakingId.value = null
                    }
                })
            }
        }
    }

    fun speak(text: String, messageId: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isTtsReady) return
        if (_isSpeaking.value && _currentSpeakingId.value == messageId) {
            stopSpeaking()
            return
        }

        stopSpeaking()
        _currentSpeakingId.value = messageId
        tts?.setSpeechRate(speed.coerceIn(0.5f, 2.0f))
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))

        // Strip markdown formatting before speaking
        val cleanText = text
            .replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " [code snippet] ")
            .replace(Regex("[#*_~`>]"), "")

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, messageId)
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, messageId)
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val result = matches?.firstOrNull().orEmpty()
                        if (result.isNotBlank()) {
                            onResult(result)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull().orEmpty()
                        if (partial.isNotBlank()) {
                            _recognizedText.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e("SpeechManager", "Error creating SpeechRecognizer", e)
            _isListening.value = false
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechManager", "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    fun destroy() {
        stopSpeaking()
        stopListening()
        tts?.shutdown()
        tts = null
    }
}
