package com.thevaguebox.probabilistictictactoe

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private lateinit var board: Array<Array<Button>>
    private var remainingX = 5
    private var remainingO = 5
    private var currentPlayer = 1
    private var currentSymbol = "X"
    private val gameState = Array(3) { Array(3) { "" } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvRemaining = findViewById<TextView>(R.id.tv_remaining)
        val tvPlayerTurn = findViewById<TextView>(R.id.tv_player_turn)
        val gridLayout = findViewById<GridLayout>(R.id.gridLayout)

        board = Array(3) { row ->
            Array(3) { col ->
                val button = gridLayout.getChildAt(row * 3 + col) as Button
                button.setOnClickListener { handleMove(button, row, col, tvRemaining, tvPlayerTurn) }
                button
            }
        }

        updateUI(tvRemaining, tvPlayerTurn)
    }

    private fun handleMove(button: Button, row: Int, col: Int, tvRemaining: TextView, tvPlayerTurn: TextView) {
        if (gameState[row][col] == "" && (remainingX > 0 || remainingO > 0)) {
            gameState[row][col] = currentSymbol
            button.text = currentSymbol
            if (currentSymbol == "X") remainingX-- else remainingO--

            if (checkWin(currentSymbol)) {
                showWinnerDialog("Player $currentPlayer won as $currentSymbol!")
                return
            }

            // Switch player
            currentPlayer = if (currentPlayer == 1) 2 else 1
            currentSymbol = getNextSymbol()

            updateUI(tvRemaining, tvPlayerTurn)
        }
    }

    private fun getNextSymbol(): String {
        val totalRemaining = remainingX + remainingO
        return if (totalRemaining == 0) ""
        else if (Random.nextDouble() < remainingX.toDouble() / totalRemaining) "X" else "O"
    }

    private fun checkWin(symbol: String): Boolean {
        for (i in 0..2) {
            if ((gameState[i][0] == symbol && gameState[i][1] == symbol && gameState[i][2] == symbol) ||
                (gameState[0][i] == symbol && gameState[1][i] == symbol && gameState[2][i] == symbol)) {
                return true
            }
        }
        return (gameState[0][0] == symbol && gameState[1][1] == symbol && gameState[2][2] == symbol) ||
                (gameState[0][2] == symbol && gameState[1][1] == symbol && gameState[2][0] == symbol)
    }

    private fun showWinnerDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("Restart") { _, _ -> resetGame() }
            .setCancelable(false)
            .show()
    }

    private fun resetGame() {
        remainingX = 5
        remainingO = 5
        currentPlayer = 1
        currentSymbol = "X"
        for (i in 0..2) {
            for (j in 0..2) {
                gameState[i][j] = ""
                board[i][j].text = ""
            }
        }
        updateUI(findViewById(R.id.tv_remaining), findViewById(R.id.tv_player_turn))
    }

    private fun updateUI(tvRemaining: TextView, tvPlayerTurn: TextView) {
        tvRemaining.text = "X: $remainingX, O: $remainingO"
        tvPlayerTurn.text = "Player $currentPlayer's Turn - Assigned: $currentSymbol"
    }
}
