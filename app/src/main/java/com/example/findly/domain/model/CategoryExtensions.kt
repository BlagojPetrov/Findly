package com.example.findly.domain.model

import android.content.Context
import com.example.findly.R

fun Category.toDisplayName(context: Context): String {
    return when (this) {
        Category.ELECTRONICS -> context.getString(R.string.category_electronics)
        Category.CLOTHING -> context.getString(R.string.category_clothing)
        Category.DOCUMENTS -> context.getString(R.string.category_documents)
        Category.ACCESSORIES -> context.getString(R.string.category_accessories)
        Category.KEYS -> context.getString(R.string.category_keys)
        Category.WALLET -> context.getString(R.string.category_wallet)
        Category.PETS -> context.getString(R.string.category_pets)
        Category.OTHER -> context.getString(R.string.category_other)
    }
}