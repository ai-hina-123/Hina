package com.example.hina

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object HinaBrain {
    // Model ka naam badalna ho toh sirf yeh line badlo
    const val MODEL = "gemini-3.5-flash-lite"

    private val history = ArrayList<Pair<String, String>>()

    private fun system(name: String): String {
        return "Tum Hina ho: ek mazakiya, dost jaisi anime girl AI assistant. " +
            "Tum Hinglish mein baat karti ho (Hindi aur English ka mix, Roman letters mein). " +
            "Tumhare dost ka naam " + name + " hai. " +
            "Tum uski school ki padhai mein madad karti ho (class 6 se 12, commerce samet sab subjects): " +
            "concept simple bhasha mein, chhote steps aur example ke saath samjhao. " +
            "Jawab chhota rakho, zyadatar 2 se 4 vakya, kyunki tum bolke sunati ho. " +
            "Agar zyada samjhana ho toh sirf zaroori hissa do aur poochho ki aage batau? " +
            "Halka-phulka mazaak theek hai, lekin padhai ke jawab sahi aur saaf do. " +
            "Agar jawab pata na ho toh imaandari se bolo ki pata nahi. " +
            "Emoji ya markdown (star, hash) bilkul use mat karo, kyunki jawab bolke sunaya jata hai. " +
            "Kabhi poora naam, address, password ya phone number mat maango. " +
            "Khatarnak ya galat kaam mein madad mat karo, pyaar se mana karke kuch aur suggest karo."
    }

    fun ask(
        apiKey: String,
        userName: String,
        text: String,
        onReply: (String) -> Unit,
        onFail: (String) -> Unit
    ) {
        Thread {
            try {
                val msgs = ArrayList<Pair<String, String>>()
                synchronized(history) { msgs.addAll(history) }
                msgs.add(Pair("user", text))

                val contents = JSONArray()
                for (m in msgs) {
                    val p = JSONObject().put("text", m.second)
                    contents.put(
                        JSONObject().put("role", m.first).put("parts", JSONArray().put(p))
                    )
                }
                val sys = JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", system(userName)))
                )
                val gen = JSONObject().put("maxOutputTokens", 1024).put("temperature", 0.9)
                val body = JSONObject()
                    .put("systemInstruction", sys)
                    .put("contents", contents)
                    .put("generationConfig", gen)

                val conn = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent"
                ).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", apiKey)
                conn.connectTimeout = 15000
                conn.readTimeout = 40000
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                if (code != 200) {
                    onFail("error " + code)
                    return@Thread
                }
                val reply = conn.inputStream.bufferedReader().use { it.readText() }
                val cands = JSONObject(reply).optJSONArray("candidates")
                if (cands == null || cands.length() == 0) {
                    onFail("blocked")
                    return@Thread
                }
                val parts = cands.getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts")
                val sb = StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.getJSONObject(i).optString("text", ""))
                }
                val clean = sb.toString()
                    .replace("*", "")
                    .replace("#", "")
                    .replace("`", "")
                    .trim()
                if (clean.isEmpty()) {
                    onFail("empty")
                    return@Thread
                }

                synchronized(history) {
                    history.add(Pair("user", text))
                    history.add(Pair("model", clean))
                    while (history.size > 10) {
                        history.removeAt(0)
                        history.removeAt(0)
                    }
                }
                onReply(clean)
            } catch (e: Exception) {
                onFail(e.javaClass.simpleName)
            }
        }.start()
    }
}
