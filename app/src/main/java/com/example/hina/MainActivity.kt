package com.example.hina

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "Hina online.\nPhase 1 complete."
        t.setTextColor(Color.CYAN)
        t.setBackgroundColor(Color.parseColor("#0A0F1E"))
        t.textSize = 28f
        t.gravity = Gravity.CENTER
        setContentView(t)
    }
}
