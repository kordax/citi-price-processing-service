package com.price.processor;

import com.price.processor.model.CcyPair;
import com.price.processor.service.ExchangeRatesGenerator;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableConfigurationProperties
@TestPropertySource("classpath:application-test.properties")
public class ExchangeRatesGeneratorTest {
	@Autowired
	private ExchangeRatesGenerator generator;

	@Test
	public void when_rates_generated_expect_changes() {
		Assertions.assertFalse(generator.getLastGeneratedRates().isEmpty());
		Map<CcyPair, Double> prevRates = generator.generateExchangeRates();
		Assertions.assertFalse(prevRates.isEmpty());

		IntStream.range(0, 10000).forEach(i -> generator.generateExchangeRates());
		Map<CcyPair, Double> lastRates = generator.getLastGeneratedRates();
		prevRates.values().forEach(Assertions::assertNotNull);
		lastRates.values().forEach(Assertions::assertNotNull);
		Assertions.assertNotEquals(prevRates.values(), lastRates.values());
	}

}
