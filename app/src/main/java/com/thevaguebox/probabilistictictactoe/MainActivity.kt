package com.thevaguebox.probabilistictictactoe

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.lottie.LottieAnimationView

class MainActivity : Activity() {

    private lateinit var gameGrid: GridLayout
    private lateinit var playerTurnTextView: TextView
    //private lateinit var remainingXTextView: TextView
    //private lateinit var remainingOTextView: TextView
    private lateinit var restartButton: Button
    private lateinit var progressX: ProgressBar
    private lateinit var progressO: ProgressBar

    private lateinit var turnIndicatorAnimationP1: LottieAnimationView
    private lateinit var turnIndicatorAnimationP2: LottieAnimationView

    private lateinit var symbolIndicatorX: LottieAnimationView
    private lateinit var symbolIndicatorO: LottieAnimationView

    private lateinit var qLearningAgent: QLearningAgent


    private lateinit var trainingManager: TrainingManager

    private var gameState = Array(3) { Array(3) { "" } }
    private var isPlayerOneTurn = true
    private var currentSymbol = "X"
    private var remainingX = 5
    private var remainingO = 5

    private var isComputerMode = 0

    private fun isWinningMove(symbol: String, row: Int, col: Int): Boolean {
        val tempBoard = gameState.map { it.clone() }.toTypedArray()
        tempBoard[row][col] = symbol
        // Check rows
        for (i in 0..2) {
            if (tempBoard[i][0] == symbol && tempBoard[i][1] == symbol && tempBoard[i][2] == symbol) return true
        }
        // Check columns
        for (j in 0..2) {
            if (tempBoard[0][j] == symbol && tempBoard[1][j] == symbol && tempBoard[2][j] == symbol) return true
        }
        // Check diagonals
        if (tempBoard[0][0] == symbol && tempBoard[1][1] == symbol && tempBoard[2][2] == symbol) return true
        if (tempBoard[0][2] == symbol && tempBoard[1][1] == symbol && tempBoard[2][0] == symbol) return true
        return false
    }

    private fun getEmptyCells(): List<Pair<Int, Int>> {
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (i in 0..2) {
            for (j in 0..2) {
                if (gameState[i][j] == "") emptyCells.add(Pair(i, j))
            }
        }
        return emptyCells
    }

    private fun makeMove(row: Int, col: Int) {
        val buttonIndex = row * 3 + col
        val button = gameGrid.getChildAt(buttonIndex) as Button
        handleMove(row, col, button)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        trainingManager = TrainingManager(this)
        trainingManager.loadWeights()

        qLearningAgent = trainingManager.agent

        setContentView(R.layout.activity_main)



        progressX = findViewById(R.id.progressX)
        progressO = findViewById(R.id.progressO)

        turnIndicatorAnimationP1 = findViewById(R.id.turnIndicatorAnimationP1)
        turnIndicatorAnimationP2 = findViewById(R.id.turnIndicatorAnimationP2)

        symbolIndicatorX = findViewById(R.id.symbolIndicatorX)
        symbolIndicatorO = findViewById(R.id.symbolIndicatorO)

        gameGrid = findViewById(R.id.gameGrid)
        playerTurnTextView = findViewById(R.id.playerTurn)

        //remainingXTextView = findViewById(R.id.remainingXText)
        //remainingOTextView = findViewById(R.id.remainingOText)

        restartButton = findViewById(R.id.restartButton)

        if (intent.getStringExtra("mode") == "computer") isComputerMode = 1
        if (intent.getStringExtra("mode") == "computerRL") isComputerMode = 2

        initializeBoard()
        updateTurnIndicator()
    }

    private fun initializeBoard() {
        gameGrid.removeAllViews()
        progressX.progress = (remainingX / 5f * 100).toInt() // Updates as Xs deplete
        progressO.progress = (remainingO / 5f * 100).toInt() // Updates as Os deplete

        symbolIndicatorX.visibility = View.VISIBLE
        symbolIndicatorX.playAnimation()
        symbolIndicatorO.visibility = View.INVISIBLE
        symbolIndicatorO.pauseAnimation()

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
            //button.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

            button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }.start()

            if (checkWinner()) {
                if(isComputerMode == 0)
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
            if (!isPlayerOneTurn) {
                if(isComputerMode == 1 )
                    Handler().postDelayed({ computerMove() }, 500)
                else if (isComputerMode == 2)
                    Handler().postDelayed({ computerRLMove() }, 500)
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

    /*private fun computerMove() {
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
    }*/

    private fun computerMove() {
        val emptyCells = getEmptyCells()
        if (emptyCells.isEmpty()) return

        // 1. Check for immediate win
        for ((row, col) in emptyCells) {
            if (isWinningMove(currentSymbol, row, col)) {
                makeMove(row, col)
                return
            }
        }

        // 2. Block player's win
        val opponentSymbol = if (currentSymbol == "X") "O" else "X"
        for ((row, col) in emptyCells) {
            if (isWinningMove(opponentSymbol, row, col)) {
                makeMove(row, col)
                return
            }
        }

        // 3. Strategic positions
        val center = Pair(1, 1)
        val corners = listOf(Pair(0, 0), Pair(0, 2), Pair(2, 0), Pair(2, 2))
        val edges = listOf(Pair(0, 1), Pair(1, 0), Pair(1, 2), Pair(2, 1))

        // Center first
        if (emptyCells.contains(center)) {
            makeMove(center.first, center.second)
            return
        }

        // Random corners
        for (corner in corners.shuffled()) {
            if (emptyCells.contains(corner)) {
                makeMove(corner.first, corner.second)
                return
            }
        }

        // Random edges
        for (edge in edges.shuffled()) {
            if (emptyCells.contains(edge)) {
                makeMove(edge.first, edge.second)
                return
            }
        }

        // Fallback (should never execute)
        makeMove(emptyCells.random().first, emptyCells.random().second)
    }

    private fun computerRLMove() {
        val emptyCells = getEmptyCells()
        if (emptyCells.isEmpty()) return

        val bestMove = emptyCells.maxByOrNull { (row, col) ->
            val tempBoard = gameState.map { it.clone() }.toTypedArray()
            tempBoard[row][col] = currentSymbol
            qLearningAgent.computeQValue(FeatureExtractor().getFeatures(tempBoard, currentSymbol))
            trainingManager.agent.computeQValue(
                FeatureExtractor().getFeatures(tempBoard, currentSymbol)
            )
        }

        bestMove?.let { (row, col) -> makeMove(row, col) }
    }

    private fun updateTurnIndicator() {
        val playerText = if (isPlayerOneTurn) "Player 1" else if (isComputerMode != 0) "Computer" else "Player 2"
        val symbolText = currentSymbol

        progressX.progress = (remainingX / 5f * 100).toInt() // Updates as Xs deplete
        progressO.progress = (remainingO / 5f * 100).toInt() // Updates as Os deplete

        if (currentSymbol == "X") {
            symbolIndicatorX.visibility = View.VISIBLE
            symbolIndicatorX.playAnimation()
            symbolIndicatorO.visibility = View.INVISIBLE
            symbolIndicatorO.pauseAnimation()
        } else {
            symbolIndicatorO.visibility = View.VISIBLE
            symbolIndicatorO.playAnimation()
            symbolIndicatorX.visibility = View.INVISIBLE
            symbolIndicatorX.pauseAnimation()
        }

        playerTurnTextView.text = "$playerText's Turn ($symbolText)"

        //remainingXTextView.text = "X: $remainingX"
        //remainingOTextView.text = "O: $remainingO"

        if (isComputerMode != 0) {
            if (isPlayerOneTurn) {
                turnIndicatorAnimationP1.visibility = View.VISIBLE
                turnIndicatorAnimationP1.playAnimation()
                turnIndicatorAnimationP2.visibility = View.INVISIBLE
                turnIndicatorAnimationP2.pauseAnimation()
            } else {
                turnIndicatorAnimationP2.visibility = View.VISIBLE
                turnIndicatorAnimationP2.playAnimation()
                turnIndicatorAnimationP1.visibility = View.INVISIBLE
                turnIndicatorAnimationP1.pauseAnimation()
            }
        } else {
            if (isPlayerOneTurn) {
                turnIndicatorAnimationP1.visibility = View.VISIBLE
                turnIndicatorAnimationP1.playAnimation()
                turnIndicatorAnimationP2.visibility = View.INVISIBLE
                turnIndicatorAnimationP2.pauseAnimation()
            } else {
                turnIndicatorAnimationP2.visibility = View.VISIBLE
                turnIndicatorAnimationP2.playAnimation()
                turnIndicatorAnimationP1.visibility = View.INVISIBLE
                turnIndicatorAnimationP1.pauseAnimation()
            }
        }

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
        val mediaPlayer = MediaPlayer.create(this, R.raw.win_sound)
        mediaPlayer.start()

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
