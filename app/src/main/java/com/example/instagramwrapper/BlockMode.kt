package com.example.instagramwrapper

enum class BlockMode {
    NORMAL,
    REELS,
}

val BlockMode.blocksReels: Boolean
    get() = this == BlockMode.REELS
