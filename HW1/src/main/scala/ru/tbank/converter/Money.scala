package ru.tbank.converter

import scala.annotation.targetName
import ru.tbank.converter.Errors.{CurrencyMismatchException, MoneyAmountShouldBeNonNegativeException, UnsupportedCurrencyException}

case class Money private (amount: BigDecimal, currency: String) {
  @targetName("add")
  def +(other: Money): Money =
    if this.currency == other.currency
    then Money(this.amount + other.amount, this.currency)
    else throw new CurrencyMismatchException()

  @targetName("sub")
  def -(other: Money): Money =
    if this.currency == other.currency
    then Money(this.amount - other.amount, this.currency)
    else throw new CurrencyMismatchException()

  def isSameCurrency(other: Money): Boolean =
    other.currency == this.currency
}

object Money {
  def apply(amount: BigDecimal, currency: String): Money =
    if amount < 0
    then throw new MoneyAmountShouldBeNonNegativeException()
    else if !Currencies.SupportedCurrencies.contains(currency)
    then throw new UnsupportedCurrencyException()
    else new Money(amount, currency)
}
