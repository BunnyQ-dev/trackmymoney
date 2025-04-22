package com.example.expensetracker

import android.content.Context

object CurrencyManager {
    private const val PREF_NAME = "CurrencyPrefs"
    private const val KEY_CURRENCY_INDEX = "currencyIndex"
    private const val KEY_CURRENCY_SYMBOL = "currencySymbol"
    
    fun getCurrencyIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CURRENCY_INDEX, 0) // Default to UAH (index 0)
    }
    
    fun setCurrencyIndex(context: Context, index: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CURRENCY_INDEX, index).apply()

        val currencySymbols = context.resources.getStringArray(R.array.currency_symbols)
        if (index in currencySymbols.indices) {
            setCurrencySymbol(context, currencySymbols[index])
        }
    }
    
    fun getCurrencySymbol(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENCY_SYMBOL, "₴") ?: "₴" // Default to UAH
    }
    
    private fun setCurrencySymbol(context: Context, symbol: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }
} 