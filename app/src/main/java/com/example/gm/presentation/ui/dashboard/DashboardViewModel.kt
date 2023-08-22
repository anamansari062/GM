package com.example.gm.presentation.ui.dashboard

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gm.BuildConfig
import com.example.gm.domain.model.Wallet
import com.example.gm.domain.use_case.basic_storage.BasicWalletStorageUseCase
import com.example.gm.domain.use_case.solana_rpc.authorize_wallet.AuthorizeWalletUseCase
import com.example.gm.domain.use_case.solana_rpc.sign_message.SignMessageUseCase
import com.example.gm.domain.use_case.solana_rpc.sign_transaction.SendTransactionUseCase
import com.example.gm.domain.use_case.solana_rpc.transactions_usecase.GetLatestBlockhashUseCase
import com.example.gm.presentation.utils.NftList
import com.google.gson.Gson
import com.solana.Solana
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authorizeWalletUseCase: AuthorizeWalletUseCase,
    private val walletStorageUseCase: BasicWalletStorageUseCase,
    private val getLatestBlockhashUseCase: GetLatestBlockhashUseCase,
    private val signMessageUseCase: SignMessageUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState = _uiState.asStateFlow()

    private val _solana = MutableLiveData<Solana>()

    private val mobileWalletAdapterClientSem =
        Semaphore(1) // allow only a single MWA connection at a time

    init {
        _solana.value = Solana(HttpNetworkingRouter(RPCEndpoint.devnetSolana))
        if (walletStorageUseCase.publicKey58 != null && walletStorageUseCase.publicKey64 != null) {
            _uiState.value.wallet = Wallet(
                publicKey58 = walletStorageUseCase.publicKey58.toString(),
                publicKey64 = walletStorageUseCase.publicKey64.toString(),
                walletStorageUseCase.balance,
            )
        } else {
            _uiState.value.wallet = null
        }
    }

    fun fetchNftList() = viewModelScope.launch {
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            val client = OkHttpClient()
            val receiver = walletStorageUseCase.publicKey58!!

            val request = Request.Builder()
                .url("https://dev.underdogprotocol.com/v2/projects/4/nfts?page=1&limit=10&ownerAddress=$receiver")
                .get()
                .addHeader("accept", "application/json")
                .addHeader(
                    "authorization",
                    "Bearer ${BuildConfig.BEARER}"
                )
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure
                    e.printStackTrace()
                    _uiState.update {
                        it.copy(
                            error = e.message ?: ""
                        )
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        val gson = Gson()
                        val nftList = gson.fromJson(responseBody, NftList::class.java)
                        // Process user data
                        Log.d(TAG, "fetchNftList: ${nftList}")
                        _uiState.update {
                            it.copy(
                                nftList = nftList,
                                isLoading = false
                            )
                        }
                    } else {
                        // Handle non-successful response
                        println("Request not successful: ${response.code}")
                    }
                }
            })

        }
    }

    companion object {
        private const val TAG = "DashboardViewModel"
        private const val LOCAL_ASSOCIATION_START_TIMEOUT_MS = 60000L
        private const val LOCAL_ASSOCIATION_CLOSE_TIMEOUT_MS = 5000L
        private const val LOCAL_ASSOCIATION_CANCEL_AFTER_WALLET_CLOSED_TIMEOUT_MS = 5000L
    }
}
