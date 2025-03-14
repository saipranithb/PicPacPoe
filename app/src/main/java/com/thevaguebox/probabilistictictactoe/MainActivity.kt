package com.thevaguebox.probabilistictictactoe

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {

    private lateinit var gameGrid: GridLayout
    private lateinit var playerTurnTextView: TextView
    private lateinit var remainingXTextView: TextView
    private lateinit var remainingOTextView: TextView
    private lateinit var restartButton: Button

    private var gameState = Array(3) { Array(3) { "" } }
    private var isPlayerOneTurn = true
    private var currentSymbol = "X"
    private var remainingX = 5
    private var remainingO = 5

    private var isComputerMode = false  //Update: Choose between two modes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gameGrid = findViewById(R.id.gameGrid)
        playerTurnTextView = findViewById(R.id.playerTurn)
        remainingXTextView = findViewById(R.id.remainingXText)
        remainingOTextView = findViewById(R.id.remainingOText)
        restartButton = findViewById(R.id.restartButton)

        isComputerMode = intent.getStringExtra("mode") == "computer"

        initializeBoard()
        updateTurnIndicator()
    }

    private fun initializeBoard() {
        gameGrid.removeAllViews()
        for (i in 0..2) {
            for (j in 0..2) {
                val cell = Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        rowSpec = GridLayout.spec(i, 1f)
                        columnSpec = GridLayout.spec(j, 1f)
                        setMargins(6, 6, 6, 6)
                    }
                    textSize = 32f
                    setTextColor(Color.BLACK)
                    background = ContextCompat.getDrawable(this@MainActivity, R.drawable.cell_background)
                    setOnClickListener { handleMove(i, j, this) }
                }
                gameGrid.addView(cell)
            }
        }
    }

    private fun handleMove(row: Int, col: Int, button: Button) {
        if (gameState[row][col] == "") {
            gameState[row][col] = currentSymbol
            button.text = currentSymbol

            button.scaleX = 0.8f
            button.scaleY = 0.8f
            button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

            if (checkWinner()) {
                if(!isComputerMode)
                    showWinnerDialog("Player ${if (isPlayerOneTurn) "1" else "2"} wins as $currentSymbol!")
                else
                    showWinnerDialog("${if (isPlayerOneTurn) "Player 1" else "Computer"} wins as $currentSymbol!")
                return
            }

            if (isBoardFull()) {
                showWinnerDialog("No winner in the round!")
                return
            }

            switchTurn()
            updateTurnIndicator()

            // If playing against AI, let the computer move after a short delay
            if (isComputerMode && !isPlayerOneTurn) {
                Handler().postDelayed({ computerMove() }, 500)
            }
        }
    }

    private fun switchTurn() {
        isPlayerOneTurn = !isPlayerOneTurn

        // Update remaining symbols
        if (currentSymbol == "X") remainingX-- else remainingO--

        // Randomly assign X or O for the new turn based on Probability
        // The way it's done will be, a random number is chosen between 1 and the sum of X and Os.
        // If the
        if ((remainingX > 0 && remainingO > 0)) {
            currentSymbol = if ((1..(remainingO + remainingX)).random() <= remainingX) "X" else "O"
        } else if (remainingX > 0) {
            currentSymbol = "X"
        } else {
            currentSymbol = "O"
        }
    }

    private fun computerMove() {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameState[i][j] == "") {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        if (emptyCells.isNotEmpty()) {
            val (row, col) = emptyCells.random() // Pick a random move
            val buttonIndex = row * 3 + col
            val button = gameGrid.getChildAt(buttonIndex) as Button
            handleMove(row, col, button)
        }
    }

    private fun updateTurnIndicator() {
        val playerText = if (isPlayerOneTurn) "Player 1" else if (isComputerMode) "Computer" else "Player 2"
        val symbolText = currentSymbol

        playerTurnTextView.text = "$playerText's Turn ($symbolText)"
        remainingXTextView.text = "X: $remainingX"
        remainingOTextView.text = "O: $remainingO"

        restartButton.setOnClickListener { resetGame() }

        // Smooth fade-in effect
        playerTurnTextView.alpha = 0f
        playerTurnTextView.animate().alpha(1f).setDuration(300).start()
    }

    private fun checkWinner(): Boolean {
        // Check rows and columns
        for (i in 0..2) {
            if (gameState[i][0] == gameState[i][1] && gameState[i][1] == gameState[i][2] && gameState[i][0] != "") return true
            if (gameState[0][i] == gameState[1][i] && gameState[1][i] == gameState[2][i] && gameState[0][i] != "") return true
        }
        // Check diagonals
        if (gameState[0][0] == gameState[1][1] && gameState[1][1] == gameState[2][2] && gameState[0][0] != "") return true
        if (gameState[0][2] == gameState[1][1] && gameState[1][1] == gameState[2][0] && gameState[0][2] != "") return true

        return false
    }

    private fun isBoardFull(): Boolean {
        for (row in gameState) {
            for (cell in row) {
                if (cell == "") return false
            }
        }
        return true
    }

    private fun showWinnerDialog(message: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_winner)

        val messageText = dialog.findViewById<TextView>(R.id.winnerMessage)
        val closeButton = dialog.findViewById<Button>(R.id.closeButton)
        val confettiAnimation = dialog.findViewById<LottieAnimationView>(R.id.confettiAnimation)

        messageText.text = message
        confettiAnimation.playAnimation()

        dialog.setCancelable(false)

        closeButton.setOnClickListener {
            dialog.dismiss()
            resetGame()
        }

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    private fun resetGame() {
        gameState = Array(3) { Array(3) { "" } }
        isPlayerOneTurn = true
        remainingX = 5
        remainingO = 5
        currentSymbol = "X"
        initializeBoard()
        updateTurnIndicator()
    }
}
