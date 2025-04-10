package com.thevaguebox.probabilistictictactoe

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrainingManager(private val context: Context) {
    val agent = QLearningAgent()
    private val featureExtractor = FeatureExtractor()
    private val prefs: SharedPreferences = context.getSharedPreferences("RLPrefs", Context.MODE_PRIVATE)

    fun trainAgent(episodes: Int, onProgress: (Int) -> Unit, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.Default).launch {
            repeat(episodes) { episode ->
                var gameState = GameState(
                    board = Array(3) { Array(3) { "" } },
                    remainingX = 5,
                    remainingO = 5,
                    currentSymbol = "X"
                )

                var isDone = false
                while (!isDone) {
                    val currentSymbol = gameState.currentSymbol
                    val opponent = if (currentSymbol == "X") "O" else "X"

                    // Explore (20%) vs Exploit (80%)
                    val (row, col) = if (Math.random() < 0.2) {
                        gameState.emptyCells().random()
                    } else {
                        gameState.emptyCells().maxByOrNull { cell ->
                            val tempBoard = gameState.board.copy { it.clone() }
                            tempBoard[cell.first][cell.second] = currentSymbol
                            agent.computeQValue(featureExtractor.getFeatures(tempBoard, currentSymbol))
                        } ?: Pair(-1, -1)
                    }

                    if (row == -1 || col == -1) break // No valid moves

                    // Update game state
                    val newBoard = gameState.board.copy { it.clone() }
                    newBoard[row][col] = currentSymbol
                    gameState = gameState.copy(
                        board = newBoard,
                        remainingX = if (currentSymbol == "X") gameState.remainingX - 1 else gameState.remainingX,
                        remainingO = if (currentSymbol == "O") gameState.remainingO - 1 else gameState.remainingO,
                        currentSymbol = opponent
                    )

                    // Calculate reward
                    val reward = when {
                        checkWin(gameState.board, currentSymbol) -> 1.0
                        checkWin(gameState.board, opponent) -> -1.0
                        else -> 0.0
                    }

                    // Update Q-values
                    val nextFeatures = featureExtractor.getFeatures(gameState.board, opponent)
                    val nextQ = agent.computeQValue(nextFeatures)
                    val currentFeatures = featureExtractor.getFeatures(newBoard, currentSymbol)
                    agent.updateWeights(currentFeatures, reward, nextQ)

                    isDone = reward != 0.0 || gameState.emptyCells().isEmpty()
                }

                withContext(Dispatchers.Main) {
                    onProgress(episode + 1)
                }
            }
            saveWeights()
            withContext(Dispatchers.Main) {
                onComplete() // Notify UI when done
            }
        }
    }

    private fun checkWin(board: Array<Array<String>>, symbol: String): Boolean {
        // Your existing win-check logic from MainActivity
        for (i in 0..2) {
            if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) return true
            if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) return true
        }
        if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) return true
        if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) return true
        return false
    }

    private fun saveWeights() {
        prefs.edit().putString("weights", agent.weights.toString()).apply()
        Log.d("RL", "Saved Weights: ${agent.weights}")
    }

    fun loadWeights() {
        val weightsStr = prefs.getString("weights", null)
        weightsStr?.let {
            agent.weights = it
                .removeSurrounding("[", "]")
                .split(", ")
                .map { num -> num.toDouble() }
                .toMutableList()
            Log.d("RL", "Loaded Weights: ${agent.weights}")
        }
    }

    // Helper data class
    data class GameState(
        val board: Array<Array<String>>,
        val remainingX: Int,
        val remainingO: Int,
        val currentSymbol: String
    ) {
        fun emptyCells(): List<Pair<Int, Int>> {
            return board.flatMapIndexed { i, row ->
                row.mapIndexedNotNull { j, cell ->
                    if (cell.isEmpty()) Pair(i, j) else null
                }
            }
        }

        fun copy(): GameState {
            return GameState(
                board = board.copy { it.clone() },
                remainingX = remainingX,
                remainingO = remainingO,
                currentSymbol = currentSymbol
            )
        }
    }
}

// Array deep copy extension
inline fun <reified T> Array<T>.copy(transform: (T) -> T): Array<T> {
    return Array(size) { transform(this[it]) }
}