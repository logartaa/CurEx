package com.my.curex.ui.exchange

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.curex.data.api.CurrencyApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val LocalExchangeViewModel = staticCompositionLocalOf<ExchangeViewModel> {
    error("No ExchangeViewModel provided")
}

@HiltViewModel
class ExchangeViewModel @Inject constructor(
    private val apiService: CurrencyApiService
) : ViewModel() {

    companion object {
        const val FREE_COMMISSION_LIMIT: Int = 5
        const val COMMISSION_RATE: Double = 0.007
        const val REFRESH_INTERVAL: Long = 5000 // 5 seconds
        const val DEFAULT_BALANCE = 1000.0
    }

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates.asStateFlow()

    var message = MutableStateFlow("")

    private var transactionsCount = 0
    var balance = mutableStateOf(mapOf("EUR" to DEFAULT_BALANCE))

    init {
        refreshExchangeRates()
    }

    private fun refreshExchangeRates() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    val rates = apiService.getExchangeRates().rates
                    _exchangeRates.value = rates
                } catch (e: Exception) {
                    Log.e("ExchangeViewModel", "Error fetching exchange rates", e)
                }
                delay(REFRESH_INTERVAL)
            }
        }
    }

    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String) {
        val rate = exchangeRates.value[toCurrency] ?: return
        val convertedAmount = amount * rate

        val commissionFee = if (transactionsCount < FREE_COMMISSION_LIMIT) 0.0 else amount * COMMISSION_RATE
        val fromBalance = balance.value[fromCurrency] ?: 0.0

        if (fromBalance < amount + commissionFee) {
            message.value = "Insufficient $fromCurrency balance."
            return
        }

        transactionsCount++

        balance.value = balance.value.toMutableMap().apply {
            this[fromCurrency] = fromBalance - amount - commissionFee
            this[toCurrency] = (this[toCurrency] ?: 0.0) + convertedAmount - commissionFee
        }

        message.value = "You have converted $amount $fromCurrency to ${"%.2f".format(convertedAmount)} $toCurrency." +
                if (commissionFee > 0) " Commission Fee - ${"%.2f".format(commissionFee)} $fromCurrency." else ""
    }

}