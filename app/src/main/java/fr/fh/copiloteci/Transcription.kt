package fr.fh.copiloteci

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Transcription en direct via le moteur de reconnaissance vocale du système
 * (aucun modèle embarqué : c'est le mode « qualité » d'Android, français).
 *
 * Le module whisper.cpp local arrivera en version suivante ; l'application
 * fonctionne sans lui (les segments peuvent aussi être saisis à la main).
 */
class Transcription(
    private val context: Context,
    private val onSegment: (String, Boolean) -> Unit
) : RecognitionListener {

    private var recognizer: SpeechRecognizer? = null
    private var actif = false
    private var derniereErreur = ""

    val disponible: Boolean get() = SpeechRecognizer.isRecognitionAvailable(context)

    fun demarrer() {
        if (actif) return
        actif = true
        lancer()
    }

    fun arreter() {
        actif = false
        runCatching { recognizer?.stopListening() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun lancer() {
        if (!actif) return
        runCatching {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
                    it.setRecognitionListener(this)
                }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            recognizer?.startListening(intent)
        }.onFailure { e ->
            dernierErreur("démarrage impossible : ${e.message}")
            actif = false
        }
    }

    private fun dernierErreur(message: String) {
        derniereErreur = message
        Log.w("CopiloteCI", "transcription : $message")
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "aucune parole reconnue"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "silence"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "moteur occupé"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "micro non autorisé"
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "réseau indisponible"
            else -> "erreur $error"
        }
        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            dernierErreur(message)
        }
        relancer()
    }

    private fun relancer() {
        if (!actif) return
        runCatching { recognizer?.cancel() }
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ lancer() }, 400)
    }

    override fun onPartialResults(partialResults: Bundle?) {
        extraire(partialResults)?.let { onSegment(it, false) }
    }

    override fun onResults(results: Bundle?) {
        extraire(results)?.let { onSegment(it, true) }
        relancer()
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    private fun extraire(bundle: Bundle?): String? {
        val listes = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return null
        return listes.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
    }
}
