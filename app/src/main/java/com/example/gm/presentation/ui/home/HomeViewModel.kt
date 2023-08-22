package com.example.gm.presentation.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gm.R
import com.example.gm.common.Constants
import com.example.gm.common.Constants.formatBalance
import com.example.gm.common.Resource
import com.example.gm.domain.model.Wallet
import com.example.gm.domain.use_case.basic_storage.BasicWalletStorageUseCase
import com.example.gm.domain.use_case.solana_rpc.authorize_wallet.AuthorizeWalletUseCase
import com.example.gm.domain.use_case.solana_rpc.sign_transaction.SendTransactionUseCase
import com.example.gm.domain.use_case.solana_rpc.transactions_usecase.BalanceUseCase
import com.example.gm.domain.use_case.solana_rpc.transactions_usecase.RequestAirdropUseCase
import com.example.gm.domain.use_case.solana_rpc.transactions_usecase.GetLatestBlockhashUseCase
import com.example.gm.presentation.utils.StartActivityForResultSender
import com.example.gm.BuildConfig
import com.example.gm.presentation.ui.extensions.openInBrowser
import com.example.gm.presentation.utils.CreateNft
import com.example.gm.presentation.utils.Nft
import com.example.gm.presentation.utils.User
import com.google.gson.Gson
import com.solana.networking.serialization.format.Borsh
import com.solana.Solana
import com.solana.api.SolanaAccountSerializer
import com.solana.api.getAccountInfo
import com.solana.core.AccountMeta
import com.solana.core.PublicKey
import com.solana.core.SerializeConfig
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.api.sendRawTransaction
import com.solana.mobilewalletadapter.clientlib.protocol.MobileWalletAdapterClient
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationIntentCreator
import com.solana.mobilewalletadapter.clientlib.scenario.LocalAssociationScenario
import com.solana.mobilewalletadapter.clientlib.scenario.Scenario
import com.solana.networking.Commitment
import com.solana.networking.HttpNetworkingRouter
import com.solana.networking.RPCEndpoint
import com.solana.networking.serialization.serializers.solana.AnchorInstructionSerializer
import com.solana.programs.SystemProgram
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authorizeWalletUseCase: AuthorizeWalletUseCase,
    private val requestAirdropUseCase: RequestAirdropUseCase,
    private val getLatestBlockhashUseCase: GetLatestBlockhashUseCase,
    private val balanceUseCase: BalanceUseCase,
    private val walletStorageUseCase: BasicWalletStorageUseCase,
    private val sendTransactionUseCase: SendTransactionUseCase

) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState = _uiState.asStateFlow()

    private val _solana = MutableLiveData<Solana>()

    private val mobileWalletAdapterClientSem =
        Semaphore(1) // allow only a single MWA connection at a time

    @OptIn(ExperimentalUnsignedTypes::class)
    private val specialNumber = uintArrayOf(1u, 7u, 33u, 37u, 57u, 69u, 75u, 100u)

    init {
        _solana.value = Solana(HttpNetworkingRouter(RPCEndpoint.devnetSolana))
        if (walletStorageUseCase.publicKey58 != null && walletStorageUseCase.publicKey64 != null) {
            _uiState.value.wallet = Wallet(
                walletStorageUseCase.publicKey58.toString(),
                walletStorageUseCase.publicKey64.toString(),
            )
            getBalance()
        } else {
            _uiState.value.wallet = null
        }
    }

    // Derives Program Derived Address for the user
    private fun getUserPDA(): PublicKey {
        return PublicKey.findProgramAddress(listOf("user".toByteArray(), PublicKey(
            walletStorageUseCase.publicKey58.toString()).toByteArray()), PublicKey(BuildConfig.PROGRAM_ID)).address
    }

    // Derives Program Derived Address for the counter
    private fun getCounterPDA(): PublicKey {
        return PublicKey.findProgramAddress(
            listOf("gm_counter".toByteArray()),
            PublicKey(BuildConfig.PROGRAM_ID)
        ).address
    }

    private fun createUser(): TransactionInstruction{
        // Defining all accounts involved in the instruction
        val keys = mutableListOf<AccountMeta>()
        keys.add(AccountMeta(getUserPDA(), false, true))
        keys.add(AccountMeta(PublicKey(walletStorageUseCase.publicKey58!!), true, true))
        keys.add(AccountMeta(SystemProgram.PROGRAM_ID, false, false))

        return TransactionInstruction(
            PublicKey(BuildConfig.PROGRAM_ID),
            keys,
            Borsh.encodeToByteArray(AnchorInstructionSerializer("create_user"), Args_createUser()))
    }

    private fun makeGm(): TransactionInstruction{
        // Defining all accounts involved in the instruction
        val keys = mutableListOf<AccountMeta>()
        keys.add(AccountMeta(getCounterPDA(), false, true))
        keys.add(AccountMeta(getUserPDA(), false, true))
        keys.add(AccountMeta(PublicKey(walletStorageUseCase.publicKey58!!), true, true))
        keys.add(AccountMeta(SystemProgram.PROGRAM_ID, false, false))

        return TransactionInstruction(
            PublicKey(BuildConfig.PROGRAM_ID),
            keys,
            Borsh.encodeToByteArray(AnchorInstructionSerializer("send_gm"), Args_sendGm()))
    }

    // Fetches the gm count and checks if it is a special number
    @OptIn(ExperimentalUnsignedTypes::class)
    private suspend fun checkSpecialNumber() {
        val count = getGmCountUser()
        if(count in specialNumber){
            _uiState.update {
                it.copy(
                    specialGm = count
                )
            }
        }
    }

    // Converts timestamp to date
    private fun getDateFromTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000) // Convert Unix timestamp to milliseconds
        val calendar = Calendar.getInstance()
        calendar.time = date

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // Month is 0-based (January is 0)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return "$day/$month/$year"
    }

    // Matches the url with the gm number
    private fun getImageURL(count: UInt): String {
        var image = ""
        if(count == 1u){
            image = BuildConfig.GM1
        }
        else if (count == 7u){
            image = BuildConfig.GM7
        }
        else if (count == 33u){
            image = BuildConfig.GM33
        }
        else if (count == 69u){
            image = BuildConfig.GM69
        }
        else if (count == 75u){
            image = BuildConfig.GM75
        }
        else if (count == 100u){
            image = BuildConfig.GM100
        }
        else{
            image = BuildConfig.GM
        }
        return image
    }

    // Mints an NFT for special rewards
    fun mintNft() = viewModelScope.launch {

        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            val user = fetchUser()
            if (user != null){
                val date = getDateFromTimestamp(user.timestamp)
                val receiver = walletStorageUseCase.publicKey58!!
                val image = getImageURL(user.gmCount)

                val client = OkHttpClient()
                val mediaType = "application/json".toMediaTypeOrNull()

                val content = "{\"attributes\":{\"gm\":\"${user.gmCount}\",\"date\":\"$date\"},\"name\":\"special number ${user.gmCount}\",\"image\":\"$image\",\"delegated\":true,\"receiverAddress\":\"$receiver\"}"
                val body = RequestBody.create(mediaType, content)
                val request = Request.Builder()
                    .url("https://dev.underdogprotocol.com/v2/projects/3/nfts")
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .addHeader("authorization", "Bearer ${BuildConfig.BEARER}")
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
                            val nft = gson.fromJson(responseBody, CreateNft::class.java)
                            fetchNft(nft.projectId, nft.nftId)
                            Log.d(TAG, "Minted NFT: $nft")

                        } else {
                            // Handle non-successful response
                            println("Request not successful: ${response.code}")
                        }
                    }
                })
            }

        }
    }

    fun fetchNft(projectId: Int, nftId: Int) {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://dev.underdogprotocol.com/v2/projects/$projectId/nfts/$nftId")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("authorization", "Bearer ${BuildConfig.BEARER}")
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
                    val nft = gson.fromJson(responseBody, Nft::class.java)
                    _uiState.update {
                        it.copy(
                            nft = nft
                        )
                    }
                    Log.d(TAG, "Minted NFT Details: $nft")

                } else {
                    // Handle non-successful response
                    println("Request not successful: ${response.code}")
                }
            }
        })
    }

    fun tweet(tweet: String) {
        _uiState.update {
            it.copy(
                tweetText = tweet
            )
        }
    }



    fun sendGm(sender: StartActivityForResultSender) = viewModelScope.launch {
        _solana.value?.let { solana ->
            withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                getLatestBlockhash(solana).let { blockHash ->
                    run {
                        when (blockHash) {
                            is Resource.Success -> {
                                Log.d(TAG, "Blockhash: ${blockHash.data}")
                                localAssociateAndExecute(sender) { client ->
                                    when (val result = authorizeWalletUseCase(client)) {
                                        is Resource.Success -> {
                                            Log.d(TAG, "Wallet connected: ${result.data}")

                                            _uiState.update {
                                                it.copy(
                                                    wallet = result.data,
                                                )
                                            }

                                            walletStorageUseCase.saveWallet(
                                                Wallet(
                                                    result.data!!.publicKey58,
                                                    result.data.publicKey64,
                                                    result.data.balance,
                                                ),
                                            )

                                            // Create instruction object
                                            val createUserInstruction = createUser()
                                            val sendGmInstruction = makeGm()

                                            // Create transaction object
                                            val transaction = Transaction()
                                            transaction.setRecentBlockHash(blockHash.data!!)
                                            transaction.addInstruction(createUserInstruction)
                                            transaction.addInstruction(sendGmInstruction)
                                            transaction.feePayer =
                                                PublicKey(walletStorageUseCase.publicKey58!!)


                                            val transactions = Array(1) {
                                                transaction.serialize(
                                                    config = SerializeConfig(
                                                        requireAllSignatures = false,
                                                    ),
                                                )
                                            }

                                            // Sending transaction object
                                            when (
                                                val message = sendTransactionUseCase(
                                                    client,
                                                    transactions,
                                                )
                                            ) {
                                                is Resource.Success -> {
                                                    com.example.gm.domain.model.Transaction(
                                                        message.data!!.signedTransaction,
                                                    ).let { transaction ->

                                                        // TODO: convert to usecase
                                                        solana.api.sendRawTransaction(message.data.signedTransaction)
                                                            .onSuccess { transactionID ->
                                                                Log.d(
                                                                    TAG,
                                                                    "Transaction sent: $transactionID",
                                                                )
                                                                _uiState.update {
                                                                    it.copy(
                                                                        transactionID = transactionID,
                                                                    )
                                                                }
                                                                checkSpecialNumber()
                                                            }
                                                            .onFailure {
                                                                if (it.message.toString().contains("0x1770")){
                                                                    _uiState.update {
                                                                        it.copy(
                                                                            error = "You have already said alot of GM today",
                                                                        )
                                                                    }
                                                                    Log.d(
                                                                        TAG,
                                                                        "You have already said alot of GM today",
                                                                    )
                                                                }
                                                                else {
                                                                    Log.d(
                                                                        TAG,
                                                                        it.localizedMessage
                                                                            ?: it.message.toString(),
                                                                    )
                                                                    _uiState.update {
                                                                        it.copy(
                                                                            error = it.toString(),
                                                                        )
                                                                    }
                                                                }

                                                            }

                                                        Log.d(
                                                            TAG,
                                                            "Transaction: ${
                                                                com.example.gm.domain.model.Transaction(
                                                                    signedTransaction = transaction.signedTransaction,
                                                                )
                                                            }",
                                                        )
                                                    }
                                                }

                                                is Resource.Loading -> {
                                                }

                                                is Resource.Error -> {
                                                    Log.e(TAG, message.message.toString())
                                                }
                                            }
                                        }
                                        is Resource.Loading -> {
                                            _uiState.value = HomeState(
                                                isLoading = true,
                                            )
                                        }
                                        is Resource.Error -> {
                                            Log.e(TAG, "Authorization failed")
                                            _uiState.value = HomeState(
                                                error = result.message
                                                    ?: "An unexpected error occurred",
                                                isLoading = false,
                                            )
                                        }
                                    }
                                }
                            }
                            is Resource.Loading -> {
                            }
                            is Resource.Error -> {
                                Log.e(TAG, "Fetch blockhash failed")
                                _uiState.value = HomeState(
                                    error = blockHash.message
                                        ?: "An unexpected error occurred",
                                    isLoading = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getGmCountUser(): UInt {
        val user = fetchUser()
        return if (user != null) {
            Log.d(TAG, "Gm count of user: ${user.gmCount}")
            _uiState.update {
                it.copy(
                    gmCurrentCount = user.currentCount
                )
            }
            user.gmCount
        } else {
            Log.d(TAG, "Gm count of user: null")
            0u
        }
    }

    private suspend fun fetchUser(): User? {
        _solana.value?.let { solana ->
            try {
                val serializer =
                    SolanaAccountSerializer((User.serializer()))
                val account = solana.api.getAccountInfo(serializer, getUserPDA()).getOrThrow()
                if (account != null) {
                    return account.data!!
                }
                else {
                    Log.d(TAG, "User: null")
                    return null
                }
            } catch (e: Exception) {
                Log.d(TAG, "Error while user: ${e}")
                return null
            }

        }
        return null
    }

    private suspend fun getLatestBlockhash(solana: Solana): Resource<String> {
        var blockHash: Resource<String> = Resource.Loading()
        getLatestBlockhashUseCase(solana).collect { result ->
            blockHash = when (result) {
                is Resource.Success -> {
                    result
                }
                is Resource.Loading -> {
                    result
                }
                is Resource.Error -> {
                    result
                }
            }
        }
        return blockHash
    }

    fun interactWallet(sender: StartActivityForResultSender) {
        if (walletStorageUseCase.publicKey58 == null) {
            connectWallet(sender)
        } else {
            clearWallet()
        }
    }

    private fun connectWallet(sender: StartActivityForResultSender) = viewModelScope.launch {
        localAssociateAndExecute(sender) { client ->

            when (val result = authorizeWalletUseCase(client)) {
                is Resource.Success -> {
                    Log.d(TAG, "Wallet connected: ${result.data}")
                    _uiState.value = HomeState(
                        wallet = result.data,
                        isLoading = false,
                    )

                    walletStorageUseCase.saveWallet(
                        result.data!!,
                    )

                    getBalance()
                }
                is Resource.Loading -> {
                    _uiState.value = HomeState(
                        isLoading = true,
                    )
                }
                is Resource.Error -> {
                    Log.e(TAG, "Authorization failed")
                    _uiState.value = HomeState(
                        error = result.message
                            ?: "An unexpected error occurred",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun getBalance() {
        viewModelScope.launch {
            _solana.value?.let {
                withContext(Dispatchers.IO) {
                    walletStorageUseCase.publicKey58?.let { publicKey ->
                        balanceUseCase(
                            it,
                            PublicKey(publicKey),
                            Commitment.CONFIRMED,
                        ).collect { result ->
                            when (result) {
                                is Resource.Success -> {
                                    // save balance to storage
                                    walletStorageUseCase.updateBalance(
                                        result.data!!.toString(),
                                    )

                                    _uiState.update {
                                        it.copy(
                                            balance = formatBalance(result.data),
                                        )
                                    }
                                }

                                is Resource.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            error = result.message
                                                ?: "An unexpected error occurred!",
                                        )
                                    }
                                }

                                is Resource.Loading -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun getWalletButtonText(context: Context): String {
        val publicKey = walletStorageUseCase.publicKey58
        return if (publicKey != null) {
            Constants.formatAddress(publicKey.toString())
        } else {
            context.getString(R.string.select_wallet)
        }
    }

    fun clearWallet() {
        walletStorageUseCase.clearWallet()
        _uiState.update {
            it.copy(
                wallet = null,
                balance = BigDecimal(0),
            )
        }
    }

    fun requestAirdrop() {
        viewModelScope.launch {
            _solana.value?.let {
                withContext(Dispatchers.IO) {
                    walletStorageUseCase.publicKey58?.let { publicKey ->
                        requestAirdropUseCase(
                            it,
                            PublicKey(publicKey),
                        ).collect { result ->
                            when (result) {
                                is Resource.Success -> {
                                    getBalance()
                                }
                                is Resource.Error -> {
                                    _uiState.update {
                                        it.copy(
                                            error = result.message
                                                ?: "An unexpected error occurred!",
                                        )
                                    }
                                }
                                is Resource.Loading -> {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = true,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun <T> localAssociateAndExecute(
        sender: StartActivityForResultSender,
        uriPrefix: Uri? = null,
        action: suspend (MobileWalletAdapterClient) -> T?,
    ): T? = coroutineScope {
        return@coroutineScope mobileWalletAdapterClientSem.withPermit {
            val localAssociation = LocalAssociationScenario(Scenario.DEFAULT_CLIENT_TIMEOUT_MS)

            val associationIntent = LocalAssociationIntentCreator.createAssociationIntent(
                uriPrefix,
                localAssociation.port,
                localAssociation.session,
            )
            try {
                sender.startActivityForResult(associationIntent) {
                    viewModelScope.launch {
                        // Ensure this coroutine will wrap up in a timely fashion when the launched
                        // activity completes
                        delay(LOCAL_ASSOCIATION_CANCEL_AFTER_WALLET_CLOSED_TIMEOUT_MS)
                        this@coroutineScope.cancel()
                    }
                }
            } catch (e: ActivityNotFoundException) {
                Log.e(TAG, "Failed to start intent=$associationIntent", e)
//                Toast.makeText(sender as Context, "msg_wallet_not_found", Toast.LENGTH_LONG).show()
                return@withPermit null
            }

            return@withPermit withContext(Dispatchers.IO) {
                try {
                    val mobileWalletAdapterClient = try {
                        runInterruptible {
                            localAssociation.start()
                                .get(LOCAL_ASSOCIATION_START_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                        }
                    } catch (e: InterruptedException) {
                        Log.w(TAG, "Interrupted while waiting for local association to be ready")
                        return@withContext null
                    } catch (e: TimeoutException) {
                        Log.e(TAG, "Timed out waiting for local association to be ready")
                        return@withContext null
                    } catch (e: ExecutionException) {
                        Log.e(TAG, "Failed establishing local association with wallet", e.cause)
                        return@withContext null
                    } catch (e: CancellationException) {
                        Log.e(TAG, "Local association was cancelled before connected", e)
                        return@withContext null
                    }

                    // NOTE: this is a blocking method call, appropriate in the Dispatchers.IO context
                    action(mobileWalletAdapterClient)
                } finally {
                    // running in Dispatchers.IO; blocking is appropriate
                    @Suppress("BlockingMethodInNonBlockingContext")
                    localAssociation.close()
                        .get(LOCAL_ASSOCIATION_CLOSE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                }
            }
        }
    }

    @Serializable
    class Args_createUser()

    @Serializable
    class Args_sendGm()

    companion object {
        private const val TAG = "HomeViewModel"
        private const val LOCAL_ASSOCIATION_START_TIMEOUT_MS = 60000L
        private const val LOCAL_ASSOCIATION_CLOSE_TIMEOUT_MS = 5000L
        private const val LOCAL_ASSOCIATION_CANCEL_AFTER_WALLET_CLOSED_TIMEOUT_MS = 5000L
    }
}
