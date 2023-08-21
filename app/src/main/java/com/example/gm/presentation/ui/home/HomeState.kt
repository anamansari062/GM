package com.example.gm.presentation.ui.home

import com.example.gm.domain.model.Wallet
import java.math.BigDecimal

data class HomeState(
    val isLoading: Boolean = false,
    val isAuthorized: Boolean = false,
    var wallet: Wallet? = null,
    val balance: BigDecimal = BigDecimal(0),
    val error: String? = null,
    val transactionID: String? = null,
    val texts: Texts = Texts(),
    val specialGm: UInt? = null,
    val gmCurrentCount: UByte? = null,
)

data class Texts(
    val walletButtonText: String = "",
    val airdropButtonText: String = "",
)
