package com.price.processor.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.price.processor.model.CcyPair;
import com.price.processor.service.ExchangeRatesGenerator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/generator")
@RequiredArgsConstructor
@Slf4j
public class GeneratorController {
	private final ExchangeRatesGenerator generator;
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@PatchMapping("generate-rates")
	public String generateRates() {
		log.info("Received `generate rates` request");

		return gson.toJson(generator.generateExchangeRates().entrySet()
			.stream()
			.collect(Collectors.toMap(
				e -> e.getKey().toString(),
				Entry::getValue
			)));
	}

	@GetMapping("get-current-rates")
	public String getCurrentRates() {
		log.info("Received `get current rates` request");

		Map<CcyPair, Double> rates = generator.getLastGeneratedRates();
		if (rates == null) {
			rates = generator.generateExchangeRates();
		}

		Map<String, Double> printableRates = rates.entrySet()
			.stream()
			.collect(Collectors.toMap(
				e -> e.getKey().toString(),
				Entry::getValue
			));

		return gson.toJson(printableRates);
	}
}
