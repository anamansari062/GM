package com.example.gm.presentation.utils

data class NftList(
    val results: List<Nft>,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
    val totalResults: Int
)

data class Nft(
    val id: Int,
    val status: String,
    val projectId: Int,
    val mintAddress: String,
    val ownerAddress: String,
    val name: String,
    val symbol: String,
    val description: String,
    val image: String,
    val attributes: Attributes
)

data class Attributes(
    val gm: String,
    val date: String
)

data class CreateNft(
    val projectId: Int,
    val transactionId: String,
    val nftId: Int,
)