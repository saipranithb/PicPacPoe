package com.thevaguebox.probabilistictictactoe

class QLearningAgent {
    var weights = mutableListOf(0.1, 0.1, 0.1) // Initial weights
    private val alpha = 0.1 // Learning rate
    private val gamma = 0.9 // Discount factor

    fun computeQValue(features: List<Double>): Double {
        return weights.zip(features).sumOf { (w, f) -> w * f }
    }

    fun updateWeights(features: List<Double>, reward: Double, nextQ: Double) {
        val predictedQ = computeQValue(features)
        val targetQ = reward + gamma * nextQ
        val delta = alpha * (targetQ - predictedQ)

        for (i in weights.indices) {
            weights[i] += delta * features[i]
        }
    }
}