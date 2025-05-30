package com.my.curex.ui.exchange

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.my.curex.R
import com.my.curex.ui.theme.Dimens

@Composable
fun ExchangeScreen(viewModel: ExchangeViewModel = hiltViewModel()) {
    val balance = viewModel.balance.value
    val currencyList = balance.filter { it.value > 0.0 }.keys.toList()
    var amount by remember { mutableStateOf("") }
    val allCurrencies = viewModel.exchangeRates.collectAsState().value.keys.toList()
    var selectedFromCurrency by remember { mutableStateOf(currencyList.firstOrNull() ?: "") }
    var selectedToCurrency by remember { mutableStateOf(allCurrencies.firstOrNull() ?: "") }
    val message = viewModel.message.collectAsState().value

    CompositionLocalProvider(LocalExchangeViewModel provides viewModel) {
        Column(modifier = Modifier.padding(Dimens.screen_padding), horizontalAlignment = Alignment.CenterHorizontally) {
            HeaderText(stringResource(R.string.my_balances))
            BalanceCard()
            Spacer(modifier = Modifier.height(Dimens.card_vertical_spacer))
            HeaderText(stringResource(R.string.currency_exchange))
            CurrencyExchangeCard(
                currencyList = currencyList,
                amount = amount,
                allCurrencies = allCurrencies,
                selectedFromCurrency = selectedFromCurrency,
                selectedToCurrency = selectedToCurrency,
                onAmountChange = { amount = it },
                onFromCurrencySelected = { selectedFromCurrency = it },
                onToCurrencySelected = { selectedToCurrency = it }
            )

            SubmitButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (amt > 0 && selectedFromCurrency != selectedToCurrency) {
                        viewModel.convertCurrency(amt, selectedFromCurrency, selectedToCurrency)
                    }
                }
            )

            if (message.isNotEmpty()) {
                MessageDialog(
                    message = message,
                    onDismiss = { viewModel.message.value= "" }
                )
            }
        }
    }
}

@Composable
fun HeaderText(headerText: String) {
    Text(
        text = headerText,
        color = Color.Black,
        modifier = Modifier.padding(Dimens.header_padding)
    )
}

@Composable
fun BalanceCard() {
    val viewModel = LocalExchangeViewModel.current
    val balance = viewModel.balance.value
    val currencyList = balance.keys.toList()
    var selectedCurrency by remember { mutableStateOf(currencyList.firstOrNull() ?: "") }
    val value = balance[selectedCurrency] ?: 0.0

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.card_elevation
        ),
        modifier = Modifier
            .fillMaxWidth()
            .size(width = Dimens.card_bal_width, height = Dimens.card_bal_height_balance)
    ) {
        CurrentAmountDisplay(
            value = "%.2f".format(value),
            currencyList = currencyList,
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { selectedCurrency = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentAmountDisplay(
    value: String,
    currencyList: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.card_bal_padding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Current Amount",
                color = Color.Gray
            )
            Text(
                text = value,
                color = Color.Black,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = selectedCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text("Currency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                currencyList.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CurrencyExchangeCard(
    currencyList: List<String>,
    amount: String,
    allCurrencies: List<String>,
    selectedFromCurrency: String,
    selectedToCurrency: String,
    onAmountChange: (String) -> Unit,
    onFromCurrencySelected: (String) -> Unit,
    onToCurrencySelected: (String) -> Unit
) {
    val viewModel = LocalExchangeViewModel.current
    val rates = viewModel.exchangeRates.collectAsState().value

    // Calculate conversion rate and converted amount
    val fromRate = rates[selectedFromCurrency] ?: 1.0
    val toRate = rates[selectedToCurrency] ?: 1.0
    val rate = toRate / fromRate
    val convertedAmount = amount.toDoubleOrNull()?.let { it * rate } ?: 0.0

    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = Dimens.card_elevation
        ),
        modifier = Modifier.fillMaxWidth()
            .size(width = Dimens.card_bal_width, height = Dimens.card_bal_height_exchange)
    ) {
        Column {
            AmountFromCurrencyRow(
                amount = amount,
                onAmountChange = onAmountChange,
                currencyList = currencyList,
                selectedCurrency = selectedFromCurrency,
                onCurrencySelected = onFromCurrencySelected
            )

            AmountToCurrencyRow(
                convertedAmount = "%.2f".format(convertedAmount),
                currencyList = allCurrencies,
                selectedCurrency = selectedToCurrency,
                onCurrencySelected = onToCurrencySelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountFromCurrencyRow(
    amount: String,
    onAmountChange: (String) -> Unit,
    currencyList: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = amount,
            onValueChange = { newValue ->
                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                    onAmountChange(newValue)
                }
            },
            label = { Text("Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            TextField(
                value = selectedCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.currency)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                currencyList.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountToCurrencyRow(
    convertedAmount: String,
    currencyList: List<String>,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = convertedAmount,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(stringResource(R.string.amount)) },
            modifier = Modifier.weight(1f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            TextField(
                value = selectedCurrency,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.currency)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                currencyList.forEach { currency ->
                    DropdownMenuItem(
                        text = { Text(currency) },
                        onClick = {
                            onCurrencySelected(currency)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SubmitButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Text(stringResource(id = R.string.submit).uppercase())
    }
}

@Composable
fun MessageDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.currency_converted)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.done))
            }
        }
    )
}