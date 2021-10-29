package com.price.processor.service;

import com.price.processor.exception.ApplicationErrorException;
import com.price.processor.model.CcyPair;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.UnknownCurrencyException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

@Slf4j
public abstract class CurrencyUtil {
	/**
	 * Converts a single currencyCode (e.g. 'USD' or 'RUB' etc.) to a {@link CurrencyUnit}.
	 *
	 * @param codePair pair of codes, e.g. 'EURUSD', 'CADRUB', consisting of 2 ISO abbrevations.
	 * @return CurrencyUnit object
	 * @throws ApplicationErrorException on error
	 */
	public static @NotNull
	CcyPair codePairToCurrPair(@NotNull String codePair) throws ApplicationErrorException {
		log.debug("Converting codePair '{}' to CurrPair", codePair);
		String[] codes = codePair.split("(?<=\\G...)");
		if (codes.length != 2) {
			throw new ApplicationErrorException(
				String.format("Invalid codePair '%s' received, number of codes provided: %s", codePair, codes.length)
			);
		}

		CurrencyUnit currencyUnitOne = currencyCodeToCurrencyUnit(codes[0]);
		CurrencyUnit currencyUnitTwo = currencyCodeToCurrencyUnit(codes[1]);

		return new CcyPair(currencyUnitOne, currencyUnitTwo);
	}

	/**
	 * Converts a single currencyCode (e.g. 'USD' or 'RUB' etc.) to a {@link CurrencyUnit}.
	 *
	 * @param currencyCode currencyCode
	 * @return CurrencyUnit object
	 * @throws ApplicationErrorException on error
	 */
	public static @NotNull CurrencyUnit currencyCodeToCurrencyUnit(@NotNull String currencyCode) throws ApplicationErrorException {
		try {
			return Monetary.getCurrency(currencyCode);
		} catch (UnknownCurrencyException uce) {
			final String errMsg = String.format("Unknown currencyCode '%s'", currencyCode);
			log.error(errMsg);

			throw new ApplicationErrorException(errMsg, uce);
		}
	}
}
