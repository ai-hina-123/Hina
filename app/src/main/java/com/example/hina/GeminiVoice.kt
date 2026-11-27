package com.example.hina

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiVoice {
    // Model ya voice badalni ho toh sirf yeh 2 lines badlo
    const val MODEL = "gemini-3.8-flash-lite-tts"
    const val VOICE = "Kore"

    fun speak(apiKey: String, text: String, onFail: (String) -> Unit) {
        Thread {
            try {
                val conn = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent"
                ).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.connectTimeout = 15000
                conn.readTimeout = 40000
                conn.doOutput = true

                val voiceName = JSONObject().put("voiceName", VOICE)
                val voiceConfig = JSONObject().put("prebuiltVoiceConfig", voiceName)
                val speechConfig = JSONObject().put("voiceConfig", voiceConfig)
                val genConfig = JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put("speechConfig", speechConfig)
                val part = JSONObject().put("text", text)
                val content = JSONObject().put("parts", JSONArray().put(part))
                val body = JSONObject()
                    .put("contents", JSONArray().put(content))
                    .put("generationConfig", genConfig)

                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code != 200) {
                    onFail("error " + code)
                    return@Thread
                }
                val reply = conn.inputStream.bufferedReader().use { it.readText() }
                val inline = JSONObject(reply)
                    .getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                    .getJSONObject("inlineData")
                val pcm = Base64.decode(inline.getString("data"), Base64.DEFAULT)
                val mime = inline.optString("mimeType", "")
                val m = Regex("rate=(\\d+)").find(mime)
                val rate = m?.groupValues?.get(1)?.toInt() ?: 24000
                play(pcm, rate)
            } catch (e: Exception) {
                onFail(e.javaClass.simpleName)
            }
        }.start()
    }

    private fun play(pcm: ByteArray, rate: Int) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(pcm.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        val ms = pcm.size.toLong() * 1000L / (rate.toLong() * 2L)
        Thread.sleep(ms + 400)
        track.release()
    }
}
