package com.price.processor.model;

import javax.money.CurrencyUnit;

public record CcyPair(CurrencyUnit unitOne, CurrencyUnit unitTwo) {
	@Override
	public String toString() {
		return this.unitOne.getCurrencyCode() + unitTwo.getCurrencyCode();
	}
}
