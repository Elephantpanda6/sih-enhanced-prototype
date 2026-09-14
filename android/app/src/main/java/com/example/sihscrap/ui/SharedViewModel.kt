package com.example.sihscrap.ui

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf

data class ScannedItem(
    val code: String,
    val name: String,
    val rust: Float,
    var weight: Double = 5.0
)

class SharedViewModel : ViewModel() {
    val cart = mutableStateListOf<ScannedItem>()

    fun addToCart(item: ScannedItem) {
        cart.add(item)
    }

    fun updateWeight(index: Int, weight: Double) {
        if (index in cart.indices) {
            cart[index] = cart[index].copy(weight = weight)
        }
    }

    fun removeFromCart(index: Int) {
        if (index in cart.indices) {
            cart.removeAt(index)
        }
    }

    fun clearCart() {
        cart.clear()
    }
}
