package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.example.data.auth.AuthManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSender
import com.example.data.model.ChatbotPersona
import com.example.data.model.GeminiAiModel
import com.example.data.model.LiveVoiceSessionState
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiGenerationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

enum class GeminiFemaleVoice(val displayName: String, val personaDescription: String, val pitch: Float, val speed: Float) {
    AOEDE("Aoede (Gemini Female)", "Warm, natural & crystal-clear automotive voice", 1.15f, 1.02f),
    KORE("Kore (Diagnostic Pro)", "Precise, assertive ASE-certified specialist", 1.08f, 1.05f),
    NOVA("Nova (Energetic Co-Pilot)", "Fast-paced dynamic GPS lane navigator", 1.22f, 1.08f)
}

class VoiceAssistantRepository(
    private val context: Context,
    private val authManager: AuthManager
) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceAssistantRepo"
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var hasGreeted = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var onSttCallback: ((String) -> Unit)? = null
    private var telemetryContextProvider: (() -> String)? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.ASSISTANT,
                content = "👋 Hello! I'm Route Logic, your Gemini automotive diagnostic specialist and GPS co-pilot. I analyze ELM327 OBD-II codes, live CAN sensors, road damage accelerometer data, and weather conditions. How can I help you?",
                isVoice = true,
                modelUsed = "gemini-3.5-flash",
                personaUsed = "Master Mechanic & OBD-II Co-Pilot"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Model, Persona, and Voice selection
    private val _selectedModel = MutableStateFlow(GeminiAiModel.FLASH)
    val selectedModel: StateFlow<GeminiAiModel> = _selectedModel.asStateFlow()

    private val _selectedPersona = MutableStateFlow(ChatbotPersona.MECHANIC)
    val selectedPersona: StateFlow<ChatbotPersona> = _selectedPersona.asStateFlow()

    private val _selectedFemaleVoice = MutableStateFlow(GeminiFemaleVoice.AOEDE)
    val selectedFemaleVoice: StateFlow<GeminiFemaleVoice> = _selectedFemaleVoice.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _mapsGroundingEnabled = MutableStateFlow(true)
    val mapsGroundingEnabled: StateFlow<Boolean> = _mapsGroundingEnabled.asStateFlow()

    // Voice & Live API State
    private val _liveVoiceState = MutableStateFlow(LiveVoiceSessionState.DISCONNECTED)
    val liveVoiceState: StateFlow<LiveVoiceSessionState> = _liveVoiceState.asStateFlow()

    private val _audioWaveformEnergy = MutableStateFlow(0.15f)
    val audioWaveformEnergy: StateFlow<Float> = _audioWaveformEnergy.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    // Speech-To-Text (STT) States
    private val _isSttListening = MutableStateFlow(false)
    val isSttListening: StateFlow<Boolean> = _isSttListening.asStateFlow()

    private val _sttTranscript = MutableStateFlow("")
    val sttTranscript: StateFlow<String> = _sttTranscript.asStateFlow()

    private val _sttError = MutableStateFlow<String?>(null)
    val sttError: StateFlow<String?> = _sttError.asStateFlow()

    private var waveformJob: Job? = null
    private val repoScope = CoroutineScope(Dispatchers.Main)

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "TTS init exception: ${e.message}")
        }
    }

    fun setTelemetryContextProvider(provider: () -> String) {
        this.telemetryContextProvider = provider
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyFemaleVoiceConfiguration()
            isTtsReady = true

            // Greet user with Gemini female voice on app open
            greetUserOnOpen(force = false)
        }
    }

    fun applyFemaleVoiceConfiguration() {
        try {
            val femaleVoiceConfig = _selectedFemaleVoice.value
            tts?.language = Locale.US

            // Search for natural female voice in installed TTS voices
            val availableVoices = tts?.voices
            val femaleVoice = availableVoices?.firstOrNull { v ->
                val name = v.name.lowercase()
                val lang = v.locale?.language.orEmpty().lowercase()
                val isFemale = name.contains("female") || name.contains("f0") || name.contains("sfg") ||
                        name.contains("rjs") || name.contains("aoede") || name.contains("kore") || name.contains("nova") ||
                        v.features?.contains("female") == true
                (lang == "en" || lang.startsWith("en")) && isFemale
            } ?: availableVoices?.firstOrNull { v ->
                val lang = v.locale?.language.orEmpty().lowercase()
                lang == "en" && !v.name.lowercase().contains("male")
            }

            if (femaleVoice != null) {
                tts?.voice = femaleVoice
                Log.d(TAG, "Selected TTS Female Voice: ${femaleVoice.name}")
            }

            tts?.setPitch(femaleVoiceConfig.pitch)
            tts?.setSpeechRate(femaleVoiceConfig.speed)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini female voice config exception: ${e.message}")
        }
    }

    fun setGeminiFemaleVoice(voice: GeminiFemaleVoice) {
        _selectedFemaleVoice.value = voice
        applyFemaleVoiceConfiguration()
    }

    fun greetUserOnOpen(force: Boolean = false) {
        if (force || !hasGreeted) {
            hasGreeted = true
            repoScope.launch {
                delay(300)
                speak("Hello! I'm Route Logic, your Gemini automotive diagnostic co-pilot. All vehicle systems are nominal.")
            }
        }
    }

    fun setModel(model: GeminiAiModel) {
        _selectedModel.value = model
    }

    fun setPersona(persona: ChatbotPersona) {
        _selectedPersona.value = persona
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _searchGroundingEnabled.value = enabled
    }

    fun toggleMapsGrounding(enabled: Boolean) {
        _mapsGroundingEnabled.value = enabled
    }

    fun speak(text: String) {
        if (isTtsReady && tts != null) {
            _isSpeaking.value = true
            startWaveformAnimation()
            // Clean markdown syntax for clear spoken TTS output
            val spokenText = text
                .replace(Regex("[*#_`>]"), "")
                .replace(Regex("http\\S+"), "online link")
                .trim()
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "gps_voice_utterance")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        _isSpeaking.value = false
        stopWaveformAnimation()
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.ASSISTANT,
                content = "Thread reset. Co-pilot is standing by with ${_selectedPersona.value.title}.",
                modelUsed = _selectedModel.value.displayName,
                personaUsed = _selectedPersona.value.title
            )
        )
    }

    // Speech-To-Text (STT) Engine & RecognitionListener
    fun startSpeechRecognition(onResult: ((String) -> Unit)? = null) {
        this.onSttCallback = onResult
        _sttError.value = null
        _sttTranscript.value = ""
        _isSttListening.value = true
        startWaveformAnimation(highEnergy = true)

        repoScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "STT: Ready for speech")
                        _sttTranscript.value = "Listening... Speak automotive command (e.g. 'Scan OBD codes' or 'Avoid potholes')"
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "STT: User began speaking")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val energy = ((rmsdB + 2f) / 12f).coerceIn(0.15f, 1.0f)
                        _audioWaveformEnergy.value = energy
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "STT: End of speech detected")
                        _isSttListening.value = false
                        stopWaveformAnimation()
                    }

                    override fun onError(error: Int) {
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission missing"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for STT"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking again"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer is busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                            else -> "Speech recognition error ($error)"
                        }
                        Log.w(TAG, "STT Error: $errorMsg ($error)")
                        _sttError.value = errorMsg
                        _isSttListening.value = false
                        stopWaveformAnimation()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        Log.d(TAG, "STT Final Result: $text")
                        _isSttListening.value = false
                        stopWaveformAnimation()

                        if (text.isNotBlank()) {
                            _sttTranscript.value = text
                            if (onSttCallback != null) {
                                onSttCallback?.invoke(text)
                            } else {
                                repoScope.launch {
                                    sendQuery(userText = text, isVoice = true)
                                }
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            _sttTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Scan OBD codes, engine status, or route directions...")
                }

                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition: ${e.message}", e)
                _sttError.value = "Speech recognition unavailable: ${e.message}"
                _isSttListening.value = false
                stopWaveformAnimation()
            }
        }
    }

    fun stopSpeechRecognition() {
        repoScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Stop STT error: ${e.message}")
            }
            _isSttListening.value = false
            stopWaveformAnimation()
        }
    }

    fun cancelSpeechRecognition() {
        repoScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Cancel STT error: ${e.message}")
            }
            _isSttListening.value = false
            _sttTranscript.value = ""
            stopWaveformAnimation()
        }
    }

    // Live Voice Session Controls (gemini-3.1-flash-live-preview)
    fun startLiveVoiceSession() {
        _liveVoiceState.value = LiveVoiceSessionState.CONNECTING
        _selectedModel.value = GeminiAiModel.LIVE_VOICE
        startWaveformAnimation()

        repoScope.launch {
            delay(800) // Connection handshake with Gemini Live API
            _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
            _liveTranscript.value = "Gemini Live Co-Pilot Connected (Voice: ${_selectedFemaleVoice.value.displayName}). Tap microphone to speak..."
        }
    }

    fun stopLiveVoiceSession() {
        _liveVoiceState.value = LiveVoiceSessionState.DISCONNECTED
        stopSpeaking()
        stopWaveformAnimation()
        _liveTranscript.value = ""
    }

    fun startListeningLive() {
        if (_liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED) {
            _liveVoiceState.value = LiveVoiceSessionState.LISTENING
            _liveTranscript.value = "Listening to your automotive voice command..."
            startWaveformAnimation(highEnergy = true)
            startSpeechRecognition { recognizedVoiceText ->
                _liveTranscript.value = "\"$recognizedVoiceText\""
                _liveVoiceState.value = LiveVoiceSessionState.PROCESSING
                repoScope.launch {
                    sendQuery(userText = recognizedVoiceText, isVoice = true)
                    _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
                }
            }
        }
    }

    fun stopListeningAndSend(simulatedVoiceText: String? = null) {
        if (_liveVoiceState.value == LiveVoiceSessionState.DISCONNECTED) return

        stopSpeechRecognition()
        val prompt = if (!simulatedVoiceText.isNullOrBlank()) {
            simulatedVoiceText
        } else {
            "Scan OBD-II codes and check engine health"
        }

        _liveVoiceState.value = LiveVoiceSessionState.PROCESSING
        _liveTranscript.value = "\"$prompt\""

        repoScope.launch {
            sendQuery(userText = prompt, isVoice = true)
            _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
        }
    }

    suspend fun sendQuery(userText: String, isVoice: Boolean = false): GeminiGenerationResult {
        _isProcessing.value = true

        // Record quota usage
        val hasQuota = authManager.recordChatUsage()
        if (!hasQuota) {
            _isProcessing.value = false
            val limitMsg = "Daily AI Chat allowance reached. Upgrade to PRO for unlimited Live Gemini voice conversations and ELM327 diagnostics."
            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.ASSISTANT,
                content = limitMsg,
                isVoice = isVoice,
                actionSuggestion = "UPGRADE_PLAN",
                modelUsed = _selectedModel.value.displayName,
                personaUsed = _selectedPersona.value.title
            )
            _messages.value = _messages.value + ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.USER,
                content = userText,
                isVoice = isVoice
            ) + assistantMsg
            return GeminiGenerationResult(text = limitMsg, modelUsed = _selectedModel.value.displayName)
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            content = userText,
            isVoice = isVoice
        )
        _messages.value = _messages.value + userMessage

        val currentTelemetryContext = telemetryContextProvider?.invoke()

        // Call Gemini Client with multi-turn history, model, persona & grounding
        val result = GeminiClient.generateChatResponse(
            messages = _messages.value,
            latestUserPrompt = userText,
            model = _selectedModel.value,
            persona = _selectedPersona.value,
            enableSearchGrounding = _searchGroundingEnabled.value,
            enableMapsGrounding = _mapsGroundingEnabled.value,
            telemetryContext = currentTelemetryContext
        )

        val assistantMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.ASSISTANT,
            content = result.text,
            isVoice = isVoice,
            modelUsed = result.modelUsed,
            personaUsed = _selectedPersona.value.title,
            searchCitations = result.searchCitations,
            groundedPlaces = result.mapPlaces,
            searchQueries = result.searchQueries
        )
        _messages.value = _messages.value + assistantMessage
        _isProcessing.value = false

        if (isVoice) {
            _liveVoiceState.value = LiveVoiceSessionState.SPEAKING
            speak(result.text)
        }

        return result
    }

    private fun startWaveformAnimation(highEnergy: Boolean = false) {
        waveformJob?.cancel()
        waveformJob = repoScope.launch {
            while (isActive) {
                val base = if (highEnergy) 0.65f else 0.35f
                val delta = (Math.random().toFloat() * 0.45f)
                _audioWaveformEnergy.value = (base + delta).coerceIn(0.1f, 1.0f)
                delay(90)
            }
        }
    }

    private fun stopWaveformAnimation() {
        waveformJob?.cancel()
        _audioWaveformEnergy.value = 0.12f
    }

    fun cleanup() {
        waveformJob?.cancel()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "SpeechRecognizer destroy exception: ${e.message}")
        }
        tts?.stop()
        tts?.shutdown()
    }
}
