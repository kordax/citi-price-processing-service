package com.price.processor.service;

import com.price.processor.config.GeneratorConfig;
import com.price.processor.event.ExchangeRatesChangedEvent;
import com.price.processor.exception.ApplicationErrorException;
import com.price.processor.model.CcyPair;
import com.price.processor.model.dto.json.JsonExchEntry;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRatesGenerator {
	private final GeneratorConfig config;
	private final JsonService jsonService;
	private final Random random = new Random();
	private Map<CcyPair, JsonExchEntry> entryMap;
	@Getter
	private Map<CcyPair, Double> lastGeneratedRates = new HashMap<>();

	@Value("classpath:exchange_rates_template.json")
	private Resource resourceFile;

	@Autowired
	private ApplicationEventPublisher eventPublisher;

	@PostConstruct
	public void init() throws IOException, ApplicationErrorException, InterruptedException {
		List<JsonExchEntry> entries = jsonService.readJsonExchangeEntries(resourceFile);
		for (JsonExchEntry entry : entries) {
			CurrencyUtil.codePairToCurrPair(entry.getPair());
		}
		entryMap = entries
			.stream()
			.collect(Collectors.toMap(
				e -> {
					try {
						return CurrencyUtil.codePairToCurrPair(e.getPair());
					} catch (ApplicationErrorException ex) {
						return new CcyPair(null, null);
					}
				},
				Function.identity()
			));

		CompletableFuture.supplyAsync(() -> {
			try {
				return run();
			} catch (InterruptedException e) {
				return null;
			}
		});
	}

	public Void run() throws InterruptedException {
		while (true) {
			Thread.sleep(config.getLinger().toMillis());
			generateExchangeRates();
		}
	}

	public Map<CcyPair, Double> generateExchangeRates() {
		log.debug("tick");
		Map<CcyPair, Double> previousRates = new HashMap<>(lastGeneratedRates);
		lastGeneratedRates = entryMap.entrySet()
			.stream()
			.collect(Collectors.toMap(
					Entry::getKey,
					e -> {
						if (!chance()) {
							return calculateRate(e.getValue().getBid(), e.getValue().getAsk());
						} else {
							return calculateRate(e.getValue().getBid(), e.getValue().getAsk()) + random.nextDouble(5.0D);
						}
					}
				)
			);

		Map<CcyPair, Double> changedRates =
			lastGeneratedRates.entrySet()
				.stream()
				.filter(e -> {
					if (previousRates.containsKey(e.getKey())) {
						return !previousRates.get(e.getKey()).equals(e.getValue());
					} else {
						return true;
					}
				})
				.collect(Collectors.toMap(
						Entry::getKey,
						Entry::getValue
					)
				);

		if (!changedRates.isEmpty()) {
			eventPublisher.publishEvent(new ExchangeRatesChangedEvent(this, changedRates));
		}

		return lastGeneratedRates;
	}

	private Double calculateRate(Double bid, Double ask) {
		return (ask / bid) * 100;
	}

	private boolean chance() {
		double d = random.nextDouble() * 100.0D;
		return d < config.getChance();
	}
}
