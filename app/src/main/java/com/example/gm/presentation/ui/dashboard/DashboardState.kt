package com.example.gm.presentation.ui.dashboard

import com.example.gm.domain.model.Wallet
import com.example.gm.presentation.utils.NftList

data class DashboardState(
    val isLoading: Boolean = true,
    var wallet: Wallet? = null,
    val error: String = "",
    val nftList: NftList? = null
)
