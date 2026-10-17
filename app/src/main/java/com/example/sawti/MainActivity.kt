package com.example.sawti

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(12, 12, 25)
        window.navigationBarColor = Color.rgb(12, 12, 25)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
            setBackgroundColor(Color.rgb(12, 12, 25))
        }

        val title = TextView(this).apply {
            text = "مساعدي الصوتي"
            textSize = 30f
            setTextColor(Color.rgb(120, 150, 255))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "مرحبًا بك! تحدث معي باللغة العربية"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        status = TextView(this).apply {
            text = "اضغط على الزر وابدأ الحديث"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(12, 32, 12, 32)
        }

        val talkButton = Button(this).apply {
            text = "🎙️ تحدث معي"
            textSize = 20f
            setOnClickListener { startListening() }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(status)
        layout.addView(talkButton)

        setContentView(layout)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) {
            val languageResult = tts.setLanguage(Locale("ar"))
            if (languageResult < TextToSpeech.LANG_AVAILABLE) {
                status.text = "خدمة الصوت العربي غير متاحة على هذا الجهاز"
            }
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "التعرّف على الصوت غير متاح على جهازك"
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }

        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)

        recognizer?.setRecognitionListener(object :
            android.speech.RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                status.text = "أنا أستمع إليك الآن..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                status.text = "جارٍ فهم كلامك..."
            }

            override fun onError(error: Int) {
                status.text = "لم أتمكن من سماعك. حاول مرة أخرى."
            }

            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()

                if (spokenText.isNullOrBlank()) {
                    status.text = "لم أفهم الكلام. حاول مجددًا."
                } else {
                    respond(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث باللغة العربية")
        }

        recognizer?.startListening(intent)
    }

    private fun respond(message: String) {
        val reply = when {
            message.contains("السلام") || message.contains("مرحبا") ->
                "وعليكم السلام ورحمة الله وبركاته، أهلًا بك!"

            message.contains("اسمك") ->
                "أنا مساعدك الصوتي، وسنعمل على تطوير قدراتي."

            message.contains("كيف حالك") ->
                "أنا بخير، شكرًا لك. كيف أستطيع مساعدتك؟"

            else ->
                "سمعتك تقول: $message. ما زلنا نجهز ربط الذكاء الاصطناعي الحقيقي."
        }

        status.text = "أنت: $message\n\nالمساعد: $reply"
        tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "assistant_reply")
    }

    override fun onDestroy() {
        recognizer?.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
