package com.example.gm.presentation.utils

import kotlinx.serialization.Serializable

@Serializable
data class Counter(
    val gm: UInt, // global gm count
    val timestamp: Long // timestamp of the recent gm
)