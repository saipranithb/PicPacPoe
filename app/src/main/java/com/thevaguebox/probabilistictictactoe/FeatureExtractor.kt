package com.thevaguebox.probabilistictictactoe

class FeatureExtractor {

    fun getFeatures(board: Array<Array<String>>, symbol: String): List<Double> {
        return listOf<Double>(
            countTwoInARow(board, symbol).toDouble(),       // Feature 1: AI's two-in-a-row
            countTwoInARow(board, opponentSymbol(symbol)).toDouble(),  // Feature 2: Opponent's two-in-a-row
            centerControl(board, symbol)                    // Feature 3: Center control
        )
    }

    private fun countTwoInARow(board: Array<Array<String>>, targetSymbol: String): Int {
        var count = 0

        // Check rows and columns
        for (i in 0..2) {
            // Rows
            if (board[i].count { it == targetSymbol } == 2 && board[i].any { it.isEmpty() }) count++
            // Columns
            if (board[0][i] == targetSymbol && board[1][i] == targetSymbol && board[2][i].isEmpty()) count++
            if (board[0][i].isEmpty() && board[1][i] == targetSymbol && board[2][i] == targetSymbol) count++
        }

        // Check diagonals
        if (board[0][0] == targetSymbol && board[1][1] == targetSymbol && board[2][2].isEmpty()) count++
        if (board[0][0].isEmpty() && board[1][1] == targetSymbol && board[2][2] == targetSymbol) count++
        if (board[0][2] == targetSymbol && board[1][1] == targetSymbol && board[2][0].isEmpty()) count++
        if (board[0][2].isEmpty() && board[1][1] == targetSymbol && board[2][0] == targetSymbol) count++

        return count
    }

    private fun centerControl(board: Array<Array<String>>, symbol: String): Double {
        return if (board[1][1] == symbol) 1.0 else 0.0
    }

    private fun opponentSymbol(symbol: String): String {
        return if (symbol == "X") "O" else "X"
    }
}