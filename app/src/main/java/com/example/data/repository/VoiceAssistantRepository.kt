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
import com.example.data.local.ChatDao
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
    private val authManager: AuthManager,
    private val chatDao: ChatDao
) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceAssistantRepo"
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var hasGreeted = false

    private var speechRecognizer: SpeechRecognizer? = null
    private var onSttCallback: ((String) -> Unit)? = null
    private var telemetryContextProvider: (() -> String)? = null

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    // Model, Persona, and Voice selection
    private val _selectedModel = MutableStateFlow(GeminiAiModel.FLASH) // Latest free model gemini-3.5-flash
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
    private val _liveVoiceState = MutableStateFlow(LiveVoiceSessionState.CONNECTED_IDLE)
    val liveVoiceState: StateFlow<LiveVoiceSessionState> = _liveVoiceState.asStateFlow()

    private val _audioWaveformEnergy = MutableStateFlow(0.15f)
    val audioWaveformEnergy: StateFlow<Float> = _audioWaveformEnergy.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _liveTranscript = MutableStateFlow("Hi, I'm GPS Route Logic A.I.. How can I help today?")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    // Speech-To-Text (STT) & Open Mic / Wake Word States
    private val _isOpenMicEnabled = MutableStateFlow(true)
    val isOpenMicEnabled: StateFlow<Boolean> = _isOpenMicEnabled.asStateFlow()

    private val _wakeWordDetected = MutableStateFlow<String?>(null)
    val wakeWordDetected: StateFlow<String?> = _wakeWordDetected.asStateFlow()

    private val _isSttListening = MutableStateFlow(false)
    val isSttListening: StateFlow<Boolean> = _isSttListening.asStateFlow()

    private val _sttTranscript = MutableStateFlow("")
    val sttTranscript: StateFlow<String> = _sttTranscript.asStateFlow()

    private val _sttError = MutableStateFlow<String?>(null)
    val sttError: StateFlow<String?> = _sttError.asStateFlow()

    private var waveformJob: Job? = null
    private var restartListeningJob: Job? = null
    private val repoScope = CoroutineScope(Dispatchers.Main)

    // Wake Word Regex Patterns ("Hey Logic", "Hi Logic", "Logic", "Hey Route Logic")
    private val wakeWordRegex = Regex("(?i)\\b(?:hey|hi|hello|ok|okay)?\\s*logic\\b")
    private val wakeWordPrefixCleanRegex = Regex("(?i)^(?:hey|hi|hello|ok|okay)?\\s*logic[,:.]?\\s*")

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "TTS init exception: ${e.message}")
        }

        // Observe persistent chat messages from Room DB
        repoScope.launch(Dispatchers.IO) {
            chatDao.getAllMessages().collect { entities ->
                if (entities.isEmpty()) {
                    val initialGreeting = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = ChatSender.ASSISTANT,
                        content = "Hi, I'm GPS Route Logic A.I.. How can I help today?",
                        isVoice = true,
                        modelUsed = "gemini-3.1-flash-live-preview",
                        personaUsed = "Master Mechanic & OBD-II Co-Pilot"
                    )
                    chatDao.insertMessage(initialGreeting.toEntity())
                } else {
                    _messages.value = entities.map { it.toChatMessage() }
                }
            }
        }
    }

    fun setTelemetryContextProvider(provider: () -> String) {
        this.telemetryContextProvider = provider
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyFemaleVoiceConfiguration()
            isTtsReady = true

            // Greet user with Gemini female voice & open mic upon app start-up
            greetUserOnOpen(force = false)
        }
    }

    fun applyFemaleVoiceConfiguration() {
        try {
            val femaleVoiceConfig = _selectedFemaleVoice.value
            tts?.language = Locale.US

            // Search for high-quality natural female voice in installed TTS voices
            val availableVoices = tts?.voices
            val highQualityFemaleVoice = availableVoices?.firstOrNull { v ->
                val name = v.name.lowercase()
                val lang = v.locale?.language.orEmpty().lowercase()
                val isUsOrEn = lang == "en" || lang.startsWith("en")
                val isNetworkOrNeural = name.contains("network") || name.contains("neural") || name.contains("sfg") || name.contains("iol") || name.contains("tpf")
                val isFemale = name.contains("female") || name.contains("f0") || name.contains("aoede") || name.contains("kore") || name.contains("nova") ||
                        v.features?.contains("female") == true
                isUsOrEn && (isFemale || isNetworkOrNeural)
            } ?: availableVoices?.firstOrNull { v ->
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

            if (highQualityFemaleVoice != null) {
                tts?.voice = highQualityFemaleVoice
                Log.d(TAG, "Selected TTS Female Voice: ${highQualityFemaleVoice.name}")
            }

            tts?.setPitch(femaleVoiceConfig.pitch)
            tts?.setSpeechRate(femaleVoiceConfig.speed)

            tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopWaveformAnimation()

                    // If open mic is enabled and live voice session is active, automatically start listening hands-free
                    if (_isOpenMicEnabled.value && _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED) {
                        repoScope.launch {
                            delay(350)
                            if (!_isSpeaking.value && !_isProcessing.value) {
                                startSpeechRecognition()
                            }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopWaveformAnimation()
                    if (_isOpenMicEnabled.value && _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED) {
                        scheduleAutoRestartListening(800)
                    }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    stopWaveformAnimation()
                    if (_isOpenMicEnabled.value && _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED) {
                        scheduleAutoRestartListening(800)
                    }
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Gemini female voice config exception: ${e.message}")
        }
    }

    fun setGeminiFemaleVoice(voice: GeminiFemaleVoice) {
        _selectedFemaleVoice.value = voice
        applyFemaleVoiceConfiguration()
    }

    fun toggleOpenMic(enabled: Boolean) {
        _isOpenMicEnabled.value = enabled
        if (enabled) {
            if (_liveVoiceState.value == LiveVoiceSessionState.DISCONNECTED) {
                startLiveVoiceSession()
            } else if (!_isSpeaking.value && !_isProcessing.value) {
                startSpeechRecognition()
            }
        } else {
            stopSpeechRecognition()
        }
    }

    fun greetUserOnOpen(force: Boolean = false) {
        if (force || !hasGreeted) {
            hasGreeted = true
            _isOpenMicEnabled.value = true
            startLiveVoiceSession()

            repoScope.launch {
                delay(300)
                speak("Hi, I'm GPS Route Logic A.I.. How can I help today?")
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
            // Clean emojis, symbols, and formatting for natural human-sounding conversation
            val spokenText = text
                .replace(Regex("[\\p{So}\\p{Cn}]"), "") // Strip emoji icons
                .replace("•", ", ")
                .replace("|", ", ")
                .replace(Regex("[*#_`>~]"), "")
                .replace(Regex("http\\S+"), "online link")
                .replace(Regex("\\s+"), " ")
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
        repoScope.launch(Dispatchers.IO) {
            chatDao.clearAllMessages()
            val initial = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.ASSISTANT,
                content = "Hi, I'm GPS Route Logic A.I.. How can I help today?",
                modelUsed = _selectedModel.value.displayName,
                personaUsed = _selectedPersona.value.title
            )
            chatDao.insertMessage(initial.toEntity())
        }
    }

    fun deleteMessage(id: String) {
        repoScope.launch(Dispatchers.IO) {
            chatDao.deleteMessage(id)
        }
    }

    // Speech-To-Text (STT) Engine & Wake Word Recognition ("Hey Logic")
    fun startSpeechRecognition(onResult: ((String) -> Unit)? = null) {
        if (_isSpeaking.value || _isProcessing.value) return

        this.onSttCallback = onResult
        _sttError.value = null
        _sttTranscript.value = ""
        _isSttListening.value = true
        _liveVoiceState.value = LiveVoiceSessionState.LISTENING
        startWaveformAnimation(highEnergy = true)

        repoScope.launch(Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "STT: Ready for speech (Open Mic Active - Wake word 'Hey Logic')")
                        _sttTranscript.value = "Listening... (Say 'Hey Logic' or speak command)"
                        _liveTranscript.value = "🎙️ Open Mic Active • Listening for 'Hey Logic' or any driving question..."
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "STT: User began speaking")
                        startWaveformAnimation(highEnergy = true)
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
                            SpeechRecognizer.ERROR_CLIENT -> "Speech recognition client ready"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "RECORD_AUDIO permission needed for Live Open Mic"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error for STT"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer resetting"
                            SpeechRecognizer.ERROR_SERVER -> "Server recognition error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout (Waiting for 'Hey Logic')"
                            else -> "Speech recognition status ($error)"
                        }
                        Log.d(TAG, "STT Status: $errorMsg ($error)")
                        _isSttListening.value = false
                        stopWaveformAnimation()

                        // If Open Mic is active, auto-restart listening for hands-free wake word recognition
                        if (_isOpenMicEnabled.value && _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED && !_isSpeaking.value && !_isProcessing.value) {
                            scheduleAutoRestartListening(1000)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        Log.d(TAG, "STT Final Result: $text")
                        _isSttListening.value = false
                        stopWaveformAnimation()

                        if (text.isNotBlank()) {
                            _sttTranscript.value = text
                            handleRecognizedVoiceInput(text)
                        } else if (_isOpenMicEnabled.value && _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED) {
                            scheduleAutoRestartListening(800)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            _sttTranscript.value = partial
                            _liveTranscript.value = "\"$partial\""

                            // Early Wake Word Check on partial stream
                            if (wakeWordRegex.containsMatchIn(partial)) {
                                _wakeWordDetected.value = "Hey Logic"
                                repoScope.launch {
                                    delay(4000)
                                    if (_wakeWordDetected.value == "Hey Logic") {
                                        _wakeWordDetected.value = null
                                    }
                                }
                            }
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'Hey Logic', check OBD codes, or ask route directions...")
                }

                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognition: ${e.message}", e)
                _sttError.value = "Speech recognition error: ${e.message}"
                _isSttListening.value = false
                stopWaveformAnimation()
            }
        }
    }

    private fun handleRecognizedVoiceInput(rawText: String) {
        val hasWakeWord = wakeWordRegex.containsMatchIn(rawText)
        if (hasWakeWord) {
            _wakeWordDetected.value = "Hey Logic"
            repoScope.launch {
                delay(4000)
                if (_wakeWordDetected.value == "Hey Logic") {
                    _wakeWordDetected.value = null
                }
            }
        }

        // Clean wake word prefix if present (e.g. "Hey Logic, scan engine codes" -> "scan engine codes")
        val strippedText = rawText.replace(wakeWordPrefixCleanRegex, "").trim()

        if (onSttCallback != null) {
            onSttCallback?.invoke(rawText)
            return
        }

        // If user said only "Hey Logic" without extra words:
        if (hasWakeWord && (strippedText.isBlank() || strippedText.equals("hey", ignoreCase = true))) {
            _liveVoiceState.value = LiveVoiceSessionState.SPEAKING
            _liveTranscript.value = "⚡ 'Hey Logic' detected! I'm listening..."
            speak("Hi, I'm GPS Route Logic A.I.. How can I help today?")
            return
        }

        val promptToSend = if (strippedText.isNotBlank()) strippedText else rawText
        _liveTranscript.value = "\"$rawText\""
        _liveVoiceState.value = LiveVoiceSessionState.PROCESSING

        repoScope.launch {
            sendQuery(userText = promptToSend, isVoice = true)
            _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
        }
    }

    private fun scheduleAutoRestartListening(delayMs: Long) {
        restartListeningJob?.cancel()
        restartListeningJob = repoScope.launch {
            delay(delayMs)
            if (_isOpenMicEnabled.value &&
                _liveVoiceState.value != LiveVoiceSessionState.DISCONNECTED &&
                !_isSpeaking.value &&
                !_isProcessing.value &&
                !_isSttListening.value
            ) {
                startSpeechRecognition()
            }
        }
    }

    fun stopSpeechRecognition() {
        restartListeningJob?.cancel()
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
        restartListeningJob?.cancel()
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
    fun startLiveVoiceSession(autoListen: Boolean = true) {
        _liveVoiceState.value = LiveVoiceSessionState.CONNECTING
        _selectedModel.value = GeminiAiModel.LIVE_VOICE
        startWaveformAnimation()

        repoScope.launch {
            delay(400) // Fast handshake with Gemini Live API
            _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
            _liveTranscript.value = "Hi, I'm GPS Route Logic A.I.. How can I help today?"

            if (autoListen && _isOpenMicEnabled.value && !_isSpeaking.value) {
                delay(300)
                startSpeechRecognition()
            }
        }
    }

    fun stopLiveVoiceSession() {
        restartListeningJob?.cancel()
        _isOpenMicEnabled.value = false
        _liveVoiceState.value = LiveVoiceSessionState.DISCONNECTED
        stopSpeaking()
        stopSpeechRecognition()
        stopWaveformAnimation()
        _liveTranscript.value = ""
    }

    fun startListeningLive() {
        if (_liveVoiceState.value == LiveVoiceSessionState.DISCONNECTED) {
            startLiveVoiceSession(autoListen = true)
        } else {
            startSpeechRecognition()
        }
    }

    fun stopListeningAndSend(simulatedVoiceText: String? = null) {
        if (_liveVoiceState.value == LiveVoiceSessionState.DISCONNECTED) {
            startLiveVoiceSession(autoListen = false)
        }

        stopSpeechRecognition()
        val prompt = if (!simulatedVoiceText.isNullOrBlank()) {
            simulatedVoiceText
        } else {
            "Hey Logic, scan OBD-II codes and check engine health"
        }

        _liveVoiceState.value = LiveVoiceSessionState.PROCESSING
        _liveTranscript.value = "\"$prompt\""

        repoScope.launch {
            val cleanPrompt = prompt.replace(wakeWordPrefixCleanRegex, "").trim()
            sendQuery(userText = if (cleanPrompt.isNotBlank()) cleanPrompt else prompt, isVoice = true)
            _liveVoiceState.value = LiveVoiceSessionState.CONNECTED_IDLE
        }
    }

    suspend fun sendQuery(userText: String, isVoice: Boolean = false): GeminiGenerationResult {
        _isProcessing.value = true
        restartListeningJob?.cancel()

        // Record quota usage
        val hasQuota = authManager.recordChatUsage()
        if (!hasQuota) {
            _isProcessing.value = false
            val currentTier = authManager.currentUser.value.tier
            val limitMsg = "Daily real-time live-chat allowance reached (${currentTier.chatTokensPerDay} daily sessions on ${currentTier.title}). Upgrade to Premium ($4.99/mo for 35 daily sessions) or PRO ($8.99/mo for Unlimited real-time Gemini chat)."
            val userMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.USER,
                content = userText,
                isVoice = isVoice
            )
            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = ChatSender.ASSISTANT,
                content = limitMsg,
                isVoice = isVoice,
                actionSuggestion = "UPGRADE_PLAN",
                modelUsed = _selectedModel.value.displayName,
                personaUsed = _selectedPersona.value.title
            )
            chatDao.insertMessage(userMsg.toEntity())
            chatDao.insertMessage(assistantMsg.toEntity())
            return GeminiGenerationResult(text = limitMsg, modelUsed = _selectedModel.value.displayName)
        }

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = ChatSender.USER,
            content = userText,
            isVoice = isVoice
        )
        chatDao.insertMessage(userMessage.toEntity())

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
        chatDao.insertMessage(assistantMessage.toEntity())
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
        restartListeningJob?.cancel()
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
