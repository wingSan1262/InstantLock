package com.risyan.quickshutdownphone.feature.model

import com.risyan.quickshutdownphone.R

enum class ShutdownType(
    val duration: Int, val imageId: Int,
    val titleId: Int, val messageId: Int,
    val timeUntilLock: Int,
) {
    NONE(
        0,
        R.drawable.lock_meme,
        0,0,
        5000
    ),
    QUICK_5_MINUTES_NFSW(
//        5
        5 * 60,
        R.drawable.lock_meme,
        R.string.you_entered_the_forbidden_land,
        R.string.denied_bonk,
        5000
    ),
    QUICK_3_MINUTES_SEXY(
        3 * 60,
        R.drawable.neuron_buster,
        R.string.woah_neuron_buster_everywhere,
        R.string.neuron_bustor_message,
        5000
    ),
    MEDIUM_10_MINUTES(
//        10
        10 * 60,
        R.drawable.lock_meme,
        0,0,
        10000
    ),
    LONG_20_MINUTES(
//        15
        20 * 60,
        R.drawable.lock_meme,
        0,0,
        10000
    ),
    NIGHT_4HOUR_TIME(
        4 * 60 * 60,
        R.drawable.lock_meme,
        0,0,
        10000
    )
}
