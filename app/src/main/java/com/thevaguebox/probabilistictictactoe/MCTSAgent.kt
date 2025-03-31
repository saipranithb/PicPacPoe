package com.thevaguebox.probabilistictictactoe

import kotlin.random.Random

class MCTSAgent(private val simulations: Int = 1000) {

    data class Node(
        val board: MutableList<Char?>,
        val move: Int?,
        var wins: Double = 0.0,
        var visits: Int = 0,
        var children: MutableList<Node> = mutableListOf(),
        var parent: Node? = null
    )

    private fun getLegalMoves(board: List<Char?>): List<Int> {
        return board.indices.filter { board[it] == null }
    }

    private fun simulate(board: MutableList<Char?>, currentPlayer: Char): Char? {
        val availableMoves = getLegalMoves(board).toMutableList()
        var player = currentPlayer
        while (availableMoves.isNotEmpty()) {
            val move = availableMoves.removeAt(Random.nextInt(availableMoves.size))
            board[move] = player
            if (checkWin(board, player)) return player
            player = if (player == 'X') 'O' else 'X'
        }
        return null // Draw
    }

    private fun expand(node: Node, currentPlayer: Char) {
        for (move in getLegalMoves(node.board)) {
            val newBoard = node.board.toMutableList()
            newBoard[move] = currentPlayer
            val child = Node(newBoard, move, parent = node)
            node.children.add(child)
        }
    }

    private fun bestChild(node: Node): Node? {
        return node.children.maxByOrNull { it.wins / it.visits + Math.sqrt(2 * Math.log(node.visits.toDouble()) / it.visits) }
    }

    private fun backpropagate(node: Node, winner: Char?) {
        var current: Node? = node
        while (current != null) {
            current.visits++
            if (winner != null && current.parent != null) {
                if (winner == 'X') current.wins += 1 // Favor X
                else if (winner == 'O') current.wins -= 1 // Penalize losing
            }
            current = current.parent
        }
    }

    fun selectBestMove(board: List<Char?>, currentPlayer: Char): Int {
        val root = Node(board.toMutableList(), null)
        for (i in 0 until simulations) {
            var node = root
            var tempBoard = node.board.toMutableList()
            var tempPlayer = currentPlayer

            // Selection
            while (node.children.isNotEmpty()) {
                node = bestChild(node) ?: break
                tempBoard[node.move!!] = tempPlayer
                tempPlayer = if (tempPlayer == 'X') 'O' else 'X'
            }

            // Expansion
            if (getLegalMoves(tempBoard).isNotEmpty()) {
                expand(node, tempPlayer)
                node = node.children.random()
                tempBoard[node.move!!] = tempPlayer
                tempPlayer = if (tempPlayer == 'X') 'O' else 'X'
            }

            // Simulation
            val winner = simulate(tempBoard, tempPlayer)

            // Backpropagation
            backpropagate(node, winner)
        }

        return root.children.maxByOrNull { it.visits }?.move ?: getLegalMoves(board).random()
    }

    private fun checkWin(board: List<Char?>, player: Char): Boolean {
        val winPatterns = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        return winPatterns.any { pattern -> pattern.all { board[it] == player } }
    }
}
