package com.lorfocus.app.ui

import androidx.annotation.DrawableRes
import com.lorfocus.app.R

/** The honey-badger emblem options. Same six marks as the design; the pick is persisted in Prefs. */
enum class Badger(val id: String, val label: String, @DrawableRes val res: Int) {
    MANTLE("mantle", "Mantle", R.drawable.badger_mantle),
    ROUNDEL("roundel", "Roundel", R.drawable.badger_roundel),
    LINE("line", "Line", R.drawable.badger_line),
    CREST("crest", "Crest", R.drawable.badger_crest),
    CLAW("claw", "Claw", R.drawable.badger_claw),
    RIDGE("ridge", "Ridge", R.drawable.badger_ridge);

    companion object {
        fun from(id: String): Badger = values().firstOrNull { it.id == id } ?: RIDGE
    }
}
