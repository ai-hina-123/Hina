package com.example.hina

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {

    private lateinit var avatar: AvatarView
    private lateinit var status: TextView
    private lateinit var replyView: TextView
    private lateinit var keyArea: LinearLayout
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    // Yahan apna pehla naam ya nickname likh sakte ho
    private val userName = "dost"

    // WhatsApp flow ki state: 0 = normal, 1 = naam, 2 = message, 3 = confirm
    private var state = 0
    private var pendingName = ""
    private var pendingNumber = ""
    private var pendingMsg = ""

    @Volatile
    private var listenAfter = false

    private val greetings = listOf(
        "Heyyy NAME! Kaise ho? Aaj kya plan hai?",
        "Arre NAME! Finally yaad aayi meri!",
        "Hello hello! Padhai ho gayi ya bahana dhundh rahe ho?",
        "Hii NAME! Main ready hoon, bolo kya karna hai?"
    )

    private val greetWords = setOf(
        "hello", "hi", "hey", "hii", "hlo", "namaste", "hina",
        "हेलो", "हैलो", "हाय", "नमस्ते", "हिना", "हलो", "हे"
    )

    private val aliases = mapOf(
        "व्हाट्सएप" to "whatsapp",
        "व्हाट्सऐप" to "whatsapp",
        "वॉट्सऐप" to "whatsapp",
        "वाट्सएप" to "whatsapp",
        "वॉट्सएप" to "whatsapp",
        "यूट्यूब" to "youtube",
        "यू ट्यूब" to "youtube",
        "फ्लिपकार्ट" to "flipkart",
        "फ्लिपकार्ड" to "flipkart",
        "इंस्टाग्राम" to "instagram",
        "फेसबुक" to "facebook",
        "क्रोम" to "chrome",
        "कैमरा" to "camera",
        "गैलरी" to "gallery",
        "सेटिंग्स" to "settings",
        "सेटिंग" to "settings",
        "जीमेल" to "gmail",
        "गूगल" to "google",
        "मैप्स" to "maps",
        "टेलीग्राम" to "telegram",
        "स्नैपचैट" to "snapchat",
        "कैलकुलेटर" to "calculator",
        "घड़ी" to "clock"
    )

    private val openWords = listOf(
        "kholo", "khol", "open", "launch", "start", "chalu", "खोलो", "खोल", "चालू", "ओपन"
    )

    private val volumeWords = listOf(
        "volume", "awaz", "awaaz", "आवाज", "वॉल्यूम", "वोल्यूम", "वाल्यूम"
    )

    private val msgWords = listOf("message", "msg", "मैसेज", "मेसेज", "संदेश")
    private val sendWords = listOf("bhej", "भेज", "send", "सेंड", "likh", "लिख")

    private val stopWords = setOf(
        "youtube", "flipkart", "yt", "hina", "हिना", "hey", "hello",
        "par", "pe", "me", "mein", "में", "पर", "पे",
        "search", "serch", "सर्च", "kar", "karo", "kro", "करो", "कर", "kardo",
        "dhundo", "dhoondo", "ढूंढो", "ढूँढो", "khojo", "खोजो", "dhundh", "ढूंढ", "ढूँढ",
        "play", "chalao", "chala", "चलाओ", "चला", "laga", "lagao", "लगाओ", "लगा",
        "do", "दो", "mujhe", "मुझे", "zara", "please", "pls", "plz"
    )

    private val cancelWords = setOf(
        "cancel", "cancel karo", "cancel kar do", "रहने दो", "rehne do",
        "छोड़ो", "chhodo", "रुको", "ruko"
    )
    private val yesWords = listOf(
        "haan", "han", "ha", "yes", "ok", "okay", "bhej", "kar do", "kardo",
        "हाँ", "हां", "हा", "ठीक", "भेज", "जी"
    )
    private val noWords = listOf(
        "nahi", "nahin", "no", "cancel", "mat", "नहीं", "मत", "रुको", "ruko"
    )

    private fun prefs(): SharedPreferences {
        return getSharedPreferences("hina", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0A0F1E"))
        root.setPadding(24, 24, 24, 24)

        avatar = AvatarView(this)
        avatar.setOnLongClickListener {
            keyArea.visibility = View.VISIBLE
            true
        }

        replyView = TextView(this)
        replyView.text = ""
        replyView.setTextColor(Color.WHITE)
        replyView.textSize = 16f
        replyView.setPadding(16, 8, 16, 8)
        val scroll = ScrollView(this)
        scroll.addView(replyView)

        status = TextView(this)
        status.text = "Button dabao aur bolo"
        status.setTextColor(Color.parseColor("#7FDBFF"))
        status.textSize = 16f
        status.gravity = Gravity.CENTER
        status.setPadding(0, 16, 0, 16)

        val btn = Button(this)
        btn.text = "🎙️ Bolo"
        btn.textSize = 20f
        btn.setOnClickListener { startListening() }

        keyArea = LinearLayout(this)
        keyArea.orientation = LinearLayout.VERTICAL
        val keyLabel = TextView(this)
        keyLabel.text = "AI brain key (Gemini)"
        keyLabel.setTextColor(Color.parseColor("#7FDBFF"))
        keyLabel.textSize = 14f
        keyLabel.gravity = Gravity.CENTER
        val keyBox = EditText(this)
        keyBox.hint = "Key yahan paste karo"
        keyBox.setHintTextColor(Color.GRAY)
        keyBox.setTextColor(Color.WHITE)
        keyBox.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        keyBox.setSingleLine(true)
        val saveBtn = Button(this)
        saveBtn.text = "Key save karo"
        saveBtn.setOnClickListener {
            val k = keyBox.text.toString().trim()
            if (k.isNotEmpty()) {
                prefs().edit().putString("gkey", k).apply()
                keyBox.setText("")
                keyArea.visibility = View.GONE
                status.text = "Key save ho gayi. Ab mujhse kuch bhi poocho!"
            }
        }
        keyArea.addView(keyLabel)
        keyArea.addView(keyBox)
        keyArea.addView(saveBtn)
        val savedKey = prefs().getString("gkey", "") ?: ""
        keyArea.visibility = if (savedKey.isEmpty()) View.VISIBLE else View.GONE

        root.addView(avatar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 0.6f))
        root.addView(status)
        root.addView(btn)
        root.addView(keyArea)
        setContentView(root)

        avatar.wave()
        avatar.say("Hello! Main Hina hoon")

        tts = TextToSpeech(this) { st ->
            if (st == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale("hi", "IN"))
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (listenAfter) {
                            listenAfter = false
                            runOnUiThread { startListening() }
                        }
                    }
                })
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
        // Contact ka naam English spelling mein sunne ke liye
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (state == 1) "en-IN" else "hi-IN")
        recognizer?.startListening(intent)
        status.text = "Sun rahi hoon..."
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (requestCode == 2) {
            status.text = if (granted) "Contacts mil gaye, ab dobara bolo" else "Contacts ki permission chahiye"
            return
        }
        if (granted) {
            startListening()
        } else {
            status.text = "Mic ki permission chahiye"
        }
    }

    private fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hina")
    }

    private fun show(reply: String) {
        replyView.text = reply
        avatar.say(reply)
        status.text = "Button dabao aur bolo"
        speak(reply)
    }

    private fun normalize(t: String): String {
        var s = t.lowercase(Locale.ROOT)
        for ((k, v) in aliases) {
            s = s.replace(k, v)
        }
        return s
    }

    private fun tokensOf(s: String): List<String> {
        return s.split(" ").map { it.trim(',', '.', '!', '?') }.filter { it.isNotEmpty() }
    }

    private fun extractQuery(s: String): String {
        return tokensOf(s)
            .filter { !stopWords.contains(it) && !openWords.contains(it) }
            .joinToString(" ")
    }

    private fun openLink(url: String, pkg: String) {
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        i.setPackage(pkg)
        try {
            startActivity(i)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun findApp(s: String): Pair<String, String>? {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
        var best: Pair<String, String>? = null
        var bestLen = 0
        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            if (pkg == packageName) continue
            val label = ri.loadLabel(pm).toString()
            val l = label.lowercase(Locale.ROOT)
            if (l.length >= 3 && s.contains(l) && l.length > bestLen) {
                best = Pair(label, pkg)
                bestLen = l.length
            }
        }
        return best
    }

    private fun findContact(name: String): Pair<String, String>? {
        val cur = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%" + name + "%"),
            null
        ) ?: return null
        var best: Pair<String, String>? = null
        cur.use { c ->
            if (c.moveToFirst()) {
                best = Pair(c.getString(0) ?: "", c.getString(1) ?: "")
            }
        }
        return best
    }

    private fun waNumber(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.length == 11 && d.startsWith("0")) d = d.substring(1)
        if (d.length == 10) d = "91" + d
        return d
    }

    private fun resetFlow() {
        state = 0
        pendingName = ""
        pendingNumber = ""
        pendingMsg = ""
    }

    private fun handleFlow(text: String, s: String): String {
        val t = s.trim()
        if (cancelWords.contains(t)) {
            resetFlow()
            return "Theek hai, cancel kar diya"
        }
        if (state == 1) {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 2)
                resetFlow()
                return "Contacts ki permission de do, phir dobara bolo"
            }
            val name = text.trim().replace(Regex("(?i)\\s+ko$"), "")
            val c = findContact(name)
            if (c == null) {
                listenAfter = true
                return "Is naam ka contact nahi mila. Naam dobara bolo, ya cancel bolo."
            }
            pendingName = c.first
            pendingNumber = waNumber(c.second)
            state = 2
            listenAfter = true
            return pendingName + " ko kya message bhejna hai?"
        }
        if (state == 2) {
            pendingMsg = text.trim()
            state = 3
            listenAfter = true
            return pendingName + " ko yeh bhejna hai: " + pendingMsg + ". Bhej doon? Haan ya nahi bolo."
        }
        val yes = yesWords.any { t.contains(it) }
        val no = noWords.any { t.contains(it) }
        if (yes && !no) {
            openLink(
                "https://wa.me/" + pendingNumber + "?text=" + Uri.encode(pendingMsg),
                "com.whatsapp"
            )
            resetFlow()
            return "WhatsApp khol diya. Message likha hua hai, bas send dabao."
        }
        resetFlow()
        return "Theek hai, message nahi bheja."
    }

    // Local command ho toh jawab deta hai, nahi toh null (tab AI brain se poochte hain)
    private fun handle(text: String): String? {
        listenAfter = false
        val s = normalize(text)
        if (state != 0) return handleFlow(text, s)

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val isVolume = volumeWords.any { s.contains(it) }
        if (isVolume) {
            val up = listOf("badha", "बढ", "up", "zyada", "ज्यादा", "increase", "full")
            val down = listOf("kam", "कम", "down", "ghata", "घटा", "decrease", "low")
            if (up.any { s.contains(it) }) {
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                return "Volume badha diya"
            }
            if (down.any { s.contains(it) }) {
                am.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                return "Volume kam kar diya"
            }
        }

        if (s.contains("home") || s.contains("होम")) {
            val h = Intent(Intent.ACTION_MAIN)
            h.addCategory(Intent.CATEGORY_HOME)
            h.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(h)
            return "Home screen par aa gayi"
        }

        if (msgWords.any { s.contains(it) } && sendWords.any { s.contains(it) }) {
            state = 1
            listenAfter = true
            return "Kisko message bhejna hai? Naam bolo. Cancel bolke band kar sakte ho."
        }

        if (s.contains("youtube")) {
            val q = extractQuery(s)
            if (q.isNotEmpty()) {
                openLink(
                    "https://www.youtube.com/results?search_query=" + Uri.encode(q),
                    "com.google.android.youtube"
                )
                return "YouTube par " + q + " search kar rahi hoon"
            }
        }

        if (s.contains("flipkart")) {
            val q = extractQuery(s)
            if (q.isNotEmpty()) {
                openLink(
                    "https://www.flipkart.com/search?q=" + Uri.encode(q),
                    "com.flipkart.android"
                )
                return "Flipkart par " + q + " dhundh rahi hoon"
            }
        }

        val wantsOpen = openWords.any { s.contains(it) }
        if (wantsOpen) {
            val app = findApp(s)
            if (app != null) {
                val launch = packageManager.getLaunchIntentForPackage(app.second)
                if (launch != null) {
                    startActivity(launch)
                    return app.first + " khol diya"
                }
            }
            return "Mujhe woh app nahi mila"
        }

        val tokens = tokensOf(s)
        if (tokens.size <= 3 && tokens.any { greetWords.contains(it) }) {
            avatar.wave()
            return greetings.random().replace("NAME", userName)
        }

        return null
    }

    private fun askBrain(text: String) {
        val key = prefs().getString("gkey", "") ?: ""
        if (key.isEmpty()) {
            keyArea.visibility = View.VISIBLE
            show("Mujhe AI brain ki key chahiye. Neeche box mein paste karo.")
            return
        }
        status.text = "Soch rahi hoon..."
        HinaBrain.ask(
            key, userName, text,
            { reply -> runOnUiThread { show(reply) } },
            { err ->
                runOnUiThread {
                    show("Mera dimaag abhi busy hai, error " + err + ". Thodi der baad try karo.")
                }
            }
        )
    }

    override fun onResults(results: Bundle?) {
        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = list?.firstOrNull()
        if (text.isNullOrEmpty()) {
            status.text = "Kuch sunai nahi diya"
            return
        }
        avatar.say(text)
        val local = handle(text)
        if (local != null) {
            show(local)
        } else {
            askBrain(text)
        }
    }

    override fun onError(error: Int) {
        status.text = "Samajh nahi aaya (error $error). Dobara try karo."
        avatar.say("Samajh nahi aaya, dobara bolo")
        listenAfter = false
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
