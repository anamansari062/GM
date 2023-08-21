package com.example.gm.presentation.utils

import com.solana.core.PublicKey
import com.solana.networking.serialization.serializers.solana.PublicKeyAs32ByteSerializer
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @Serializable(with = PublicKeyAs32ByteSerializer::class) val author: PublicKey,
    val currentCount: UByte, // number of times they have said gm today
    val gmCount: UInt, // gm count of current day
    val timestamp: Long // Timestamp for current day gm
)