package com.flowfoundation.wallet.utils

import com.flowfoundation.wallet.manager.price.CurrencyManager
import com.flowfoundation.wallet.page.profile.subpage.currency.model.selectedCurrency
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun Float.formatPrice(
    digits: Int = 3,
    convertCurrency: Boolean = true,
    includeSymbol: Boolean = false,
    includeSymbolSpace: Boolean = false,
    isAbbreviation: Boolean = false
): String {
    var value = this
    if (convertCurrency) {
        if (CurrencyManager.currencyPrice() < 0) {
            return "-"
        }
        value *= CurrencyManager.currencyPrice()
    }
    val format = if (value < 1_000_000) {
        value.format(digits)
    } else if (isAbbreviation) {
        value.toDouble().formatLargeNumber()
    } else {
        value.toDouble().formatNumberWithCommas()
    }
    return if (includeSymbol) "${selectedCurrency().symbol}${if (includeSymbolSpace) " " else ""}$format" else format
}

fun BigDecimal.formatPrice(
    digits: Int = 3,
    convertCurrency: Boolean = true,
    includeSymbol: Boolean = false,
    includeSymbolSpace: Boolean = false,
    isAbbreviation: Boolean = false
): String {
    var value = this
    if (convertCurrency) {
        if (CurrencyManager.currencyDecimalPrice() < BigDecimal.ZERO) {
            return "-"
        }
        value *= CurrencyManager.currencyDecimalPrice()
    }
    val format = if (value < BigDecimal("1000000")) {
        value.format(digits)
    } else if (isAbbreviation) {
        value.formatLargeNumber()
    } else {
        value.formatNumberWithCommas()
    }
    return if (includeSymbol) "${selectedCurrency().symbol}${if (includeSymbolSpace) " " else ""}$format" else format
}

fun BigDecimal.format(
    digits: Int = 3,
    roundingMode: RoundingMode = RoundingMode.DOWN
): String {
    return this.setScale(digits, roundingMode).stripTrailingZeros().toPlainString()
}

fun BigDecimal.formatLargeBalanceNumber(isAbbreviation: Boolean = false): String {
    return if (this < BigDecimal("1000000")) {
        this.format()
    } else if (isAbbreviation) {
        this.formatLargeNumber()
    } else {
        this.formatNumberWithCommas()
    }
}

fun BigDecimal.formatLargeNumber(): String {
    val decimalFormat = DecimalFormat("0.###")
    decimalFormat.roundingMode = RoundingMode.DOWN
    return when {
        this >= BigDecimal("1000000000000") -> decimalFormat.format(this.divide(BigDecimal("1000000000000"))) + "t"
        this >= BigDecimal("1000000000") -> decimalFormat.format(this.divide(BigDecimal("1000000000"))) + "b"
        this >= BigDecimal("1000000") -> decimalFormat.format(this.divide(BigDecimal("1000000"))) + "m"
        else -> this.format()
    }
}

fun BigDecimal.formatNumberWithCommas(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(this)
}

fun Float.format(digits: Int = 3, roundingMode: RoundingMode = RoundingMode.DOWN): String {
    return DecimalFormat("0.${"#".repeat(digits)}").apply { setRoundingMode(roundingMode) }.format(this)
}

fun Float.formatNum(
    digits: Int = 3,
    roundingMode: RoundingMode = RoundingMode.DOWN,
): String {
    return format(digits, roundingMode)
}

fun Float.formatLargeBalanceNumber(isAbbreviation: Boolean = false): String {
    return if (this < 1_000_000) {
        this.format()
    } else if (isAbbreviation) {
        this.toDouble().formatLargeNumber()
    } else {
        this.toDouble().formatNumberWithCommas()
    }
}

fun Double.formatLargeNumber(): String {
    val decimalFormat = DecimalFormat("0.###")
    decimalFormat.roundingMode = RoundingMode.DOWN
    return when {
        this >= 1_000_000_000_000 -> decimalFormat.format(this / 1_000_000_000_000) + "t"
        this >= 1_000_000_000 -> decimalFormat.format(this / 1_000_000_000) + "b"
        this >= 1_000_000 -> decimalFormat.format(this / 1_000_000) + "m"
        else -> this.toString()
    }
}

fun Double.formatNumberWithCommas(): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return formatter.format(this)
}

fun Double.formatPrice(
    digits: Int = 3,
    convertCurrency: Boolean = true,
    includeSymbol: Boolean = false,
    includeSymbolSpace: Boolean = false,
): String {
    var value = this
    if (convertCurrency) {
        if (CurrencyManager.currencyPrice() < 0) {
            return "-"
        }
        value *= CurrencyManager.currencyPrice()
    }
    val format = value.format(digits)
    return if (includeSymbol) "${selectedCurrency().symbol}${if (includeSymbolSpace) " " else ""}$format" else format
}

fun Double.format(digits: Int = 3, roundingMode: RoundingMode = RoundingMode.DOWN): String {
    return DecimalFormat("0.${"#".repeat(digits)}").apply { setRoundingMode(roundingMode) }.format(this)
}

fun Double.formatNum(
    digits: Int = 3,
    roundingMode: RoundingMode = RoundingMode.DOWN,
): String {
    return format(digits, roundingMode)
}