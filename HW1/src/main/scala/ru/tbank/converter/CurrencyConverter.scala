package ru.tbank.converter

import Errors.{UnsupportedCurrencyException, SameCurrencyExchangeException}

class CurrencyConverter(ratesDictionary: Map[String, Map[String, BigDecimal]]) {
  def exchange(money: Money, toCurrency: String): Money =
    if money.currency == toCurrency
    then throw new SameCurrencyExchangeException()
    else Money(money.amount * ratesDictionary(money.currency)(toCurrency), toCurrency)
}

object CurrencyConverter {

  import Currencies.SupportedCurrencies

  def apply(ratesDictionary: Map[String, Map[String, BigDecimal]]): CurrencyConverter = {
    val fromCurrencies = ratesDictionary.keys
    val toCurrencies = ratesDictionary.values
    if (
      fromCurrencies.toSet
        .subsetOf(SupportedCurrencies) && toCurrencies.forall(_.keys.toSet.subsetOf(SupportedCurrencies))
    ) new CurrencyConverter(ratesDictionary)
    else throw new UnsupportedCurrencyException()
  }
}
