package com.example.proyecto.domain

import kotlin.random.Random

object FakeMushroomClassifier {

    private val labels = listOf(
        "Amanita muscaria",
        "Boletus edulis",
        "Cantharellus cibarius",
        "Agaricus campestris"
    )

    fun classify(): Pair<String, Float> {
        val label = labels.random()
        val confidence = Random.nextDouble(0.75, 0.99).toFloat()
        return label to confidence
    }
}
