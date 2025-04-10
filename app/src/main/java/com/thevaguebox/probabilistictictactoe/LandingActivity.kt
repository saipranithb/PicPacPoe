package com.thevaguebox.probabilistictictactoe

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LandingActivity : Activity() {
    private lateinit var btnOffline: Button
    private lateinit var btnComputer: Button
    private lateinit var btnComputerRL: Button
    private lateinit var btnTrain: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        btnOffline = findViewById(R.id.btnOffline)
        btnComputer = findViewById(R.id.btnComputer)
        btnComputerRL = findViewById(R.id.btnComputerRL)
        btnTrain = findViewById(R.id.btnTrain)

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

        btnComputerRL.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("mode", "computerRL")
            startActivity(intent)
        }

        btnTrain.setOnClickListener {
            showTrainingDialog() // Show progress dialog
        }
    }

    private fun showTrainingDialog() {
        val dialog = Dialog(this).apply {
            setContentView(R.layout.dialog_training)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        val progressText = dialog.findViewById<TextView>(R.id.tvProgress)
        val trainingManager = TrainingManager(this)

        trainingManager.trainAgent(
            episodes = 10000,
            onProgress = { progress ->
                progressText.text = "Training: $progress/10000 episodes"
            },
            onComplete = {
                dialog.dismiss() // Close dialog when done
                Toast.makeText(this, "Training Complete!", Toast.LENGTH_SHORT).show()
            }
        )

        dialog.show()
    }
}
