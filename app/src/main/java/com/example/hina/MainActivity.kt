package com.example.hina

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {

    private lateinit var status: TextView
    private lateinit var result: TextView
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(Color.parseColor("#0A0F1E"))
        root.setPadding(48, 48, 48, 48)

        val title = TextView(this)
        title.text = "HINA"
        title.setTextColor(Color.CYAN)
        title.textSize = 36f
        title.gravity = Gravity.CENTER

        status = TextView(this)
        status.text = "Button dabao aur bolo"
        status.setTextColor(Color.parseColor("#7FDBFF"))
        status.textSize = 18f
        status.gravity = Gravity.CENTER
        status.setPadding(0, 32, 0, 32)

        result = TextView(this)
        result.text = ""
        result.setTextColor(Color.WHITE)
        result.textSize = 22f
        result.gravity = Gravity.CENTER
        result.setPadding(0, 16, 0, 48)

        val btn = Button(this)
        btn.text = "🎙️ Bolo"
        btn.textSize = 20f
        btn.setOnClickListener { startListening() }

        root.addView(title)
        root.addView(status)
        root.addView(result)
        root.addView(btn)
        setContentView(root)

        tts = TextToSpeech(this) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("hi", "IN"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
            }
        }
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Is phone mein speech recognition nahi mila"
            return
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(this)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
        recognizer?.startListening(intent)
        status.text = "Sun rahi hoon..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            status.text = "Mic ki permission chahiye"
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hina")
    }

    override fun onResults(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = list?.firstOrNull()
        if (text.isNullOrEmpty()) {
            status.text = "Kuch sunai nahi diya"
            return
        }
        status.text = "Maine suna:"
        result.text = text
        speak("Aapne kaha: $text")
    }

    override fun onError(error: Int) {
        status.text = "Samajh nahi aaya (error $error). Dobara try karo."
        speak("Mujhe samajh nahi aaya")
    }

    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onDestroy() {
        super.onDestroy()
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
