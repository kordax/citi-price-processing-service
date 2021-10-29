package com.price.processor.service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DummyPriceProcessor implements PriceProcessor {
	private long operationTimeMs;

	public DummyPriceProcessor(long operationTimeMs) {
		this.operationTimeMs = operationTimeMs;
	}

	public void onPrice(String ccyPair, double rate) {
		log.info("Processing ccyPair '{}', rate '{}', operationTime '{}'", ccyPair, rate, operationTimeMs);
		try {
			Thread.sleep(operationTimeMs);
		} catch (InterruptedException e) {
			log.error("IE error", e);
		}
	}

	public void subscribe(PriceProcessor priceProcessor) {
		log.info("Subscribe on Dummy processor");
	}

	public void unsubscribe(PriceProcessor priceProcessor) {
		log.info("Unsubscribe on Dummy processor");
	}

	@Override
	public boolean cancel() {
		log.info("I'm cancelling the operation!");

		return true;
	}
}