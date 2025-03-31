package com.thevaguebox.probabilistictictactoe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LandingActivity : Activity() {
    private lateinit var btnOffline: Button
    private lateinit var btnComputer: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        btnOffline = findViewById(R.id.btnOffline)
        btnComputer = findViewById(R.id.btnComputer)

        btnOffline.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("mode", "offline")
            startActivity(intent)
        }

        btnComputer.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("mode", "computer")
            startActivity(intent)
        }
    }
}
