package com.example.expensetracker

import android.content.Context
import android.content.SharedPreferences

object SharedPrefManager {
    private const val PREF_NAME = "ExpenseTrackerPrefs"
    private const val KEY_IS_FIRST_LAUNCH = "isFirstLaunch"
    private const val KEY_CURRENCY_INDEX = "currencyIndex"
    private const val KEY_CURRENCY_SYMBOL = "currencySymbol"
    
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun isFirstLaunch(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted(context: Context) {
        getSharedPreferences(context).edit().putBoolean(KEY_IS_FIRST_LAUNCH, false).apply()
    }
    
    fun getCurrencyIndex(context: Context): Int {
        return getSharedPreferences(context).getInt(KEY_CURRENCY_INDEX, 0)
    }
    
    fun setCurrencyIndex(context: Context, index: Int) {
        getSharedPreferences(context).edit().putInt(KEY_CURRENCY_INDEX, index).apply()

        val currencySymbols = context.resources.getStringArray(R.array.currency_symbols)
        if (index in currencySymbols.indices) {
            setCurrencySymbol(context, currencySymbols[index])
        }
    }
    
    fun getCurrencySymbol(context: Context): String {
        return getSharedPreferences(context).getString(KEY_CURRENCY_SYMBOL, "₴") ?: "₴"
    }
    
    private fun setCurrencySymbol(context: Context, symbol: String) {
        getSharedPreferences(context).edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }
}
