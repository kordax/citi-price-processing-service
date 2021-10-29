package com.price.processor.service;

import com.price.processor.config.ExchangeRatesConfig;
import com.price.processor.event.ExchangeRatesChangedEvent;
import com.price.processor.model.CcyPair;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRatesMonitor implements ApplicationListener<ExchangeRatesChangedEvent> {
	private final ExchangeRatesConfig config;
	private Map<CcyPair, Double> snapshot = new HashMap<>();
	private Map<CcyPair, Long> lastChangedTimestamp = new HashMap<>();

	@Override
	public void onApplicationEvent(ExchangeRatesChangedEvent event) {
		log.debug("Received ExchangeRatesGenerationEvent");
		final Map<CcyPair, Double> rates = event.getRates();
		rates.forEach(
			(eventPair, eventRate) -> {
				if (!snapshot.containsKey(eventPair)) {
					lastChangedTimestamp.put(eventPair, System.currentTimeMillis());
				} else {
					final Double rate = snapshot.get(eventPair);
					if (!rate.equals(eventRate)) {
						lastChangedTimestamp.put(eventPair, System.currentTimeMillis());
					}
				}
			}
		);
	}

	public boolean isRare(CcyPair ccyPair) {
		if (lastChangedTimestamp.containsKey(ccyPair)) {
			return System.currentTimeMillis() - lastChangedTimestamp.get(ccyPair) > config.getRareChangingThreshold().toMillis();
		}

		return false;
	}
}
