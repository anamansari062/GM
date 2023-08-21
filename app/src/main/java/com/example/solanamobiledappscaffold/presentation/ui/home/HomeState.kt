package com.example.solanamobiledappscaffold.presentation.ui.home

import com.example.solanamobiledappscaffold.domain.model.Wallet
import java.math.BigDecimal

data class HomeState(
    val isLoading: Boolean = false,
    val isAuthorized: Boolean = false,
    var wallet: Wallet? = null,
    val balance: BigDecimal = BigDecimal(0),
    val error: String? = null,
    val transactionID: String? = null,
    val texts: Texts = Texts(),
    val gmCount: UInt? = null,
    val gmGlobalCount: UInt? = null,
    val specialNumber: Boolean? = null,
    val refresh: Boolean? = null
)

data class Texts(
    val walletButtonText: String = "",
    val airdropButtonText: String = "",
)
