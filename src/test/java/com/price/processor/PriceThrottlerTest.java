package com.price.processor;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.price.processor.config.ExchangeRatesConfig;
import com.price.processor.config.PriceThrottlerConfig;
import com.price.processor.service.DummyPriceProcessor;
import com.price.processor.service.ExchangeRatesGenerator;
import com.price.processor.service.PriceProcessor;
import com.price.processor.service.PriceThrottler;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EnableConfigurationProperties
@TestPropertySource("classpath:application-test.properties")
public class PriceThrottlerTest {
	private final static Logger log = LoggerFactory.getLogger(PriceThrottlerTest.class);

	@Autowired
	ExchangeRatesConfig exchangeRatesConfig;

	@Autowired
	PriceThrottlerConfig priceThrottlerConfig;

	@SpyBean
	PriceThrottler throttler;

	@SpyBean
	ExchangeRatesGenerator generator;

	@Value("${com.price.processor.throttler.soft-timeout}")
	Duration softTimeout;

	@BeforeEach
	public void setup() {
		throttler.getSubscribers().forEach(p -> throttler.unsubscribe(p));
	}

	@Test
	public void when_one_subscriber_is_slow_expect_throttling_to_occur() {
		priceThrottlerConfig.setSoftTimeout(Duration.ofMillis(100L));
		exchangeRatesConfig.setRareChangingThreshold(Duration.ofDays(1L));
		for (int i = 0; i < 3; i++) {
			long lingerMs = 0L;
			log.info("Subscribing fast processor with operationTimeMs {}", lingerMs);
			PriceProcessor processor = new DummyPriceProcessor(lingerMs);

			throttler.subscribe(processor);
		}

		final long slowLingerMs = softTimeout.toMillis() + Duration.ofSeconds(3L).toMillis();
		log.info("Subscribing slow processor with operationTimeMs {}", slowLingerMs);
		PriceProcessor processor = new DummyPriceProcessor(slowLingerMs);

		throttler.subscribe(processor);

		verify(throttler, timeout(Duration.ofMillis(slowLingerMs + 10000L).toMillis()).atLeastOnce()).onSkip();
	}

	@Test
	public void when_rare_rate_received_expect_cancel_to_occur() {
		exchangeRatesConfig.setRareChangingThreshold(Duration.ofMillis(0L));
		PriceProcessor processor = Mockito.spy(new DummyPriceProcessor(1000L));
		throttler.subscribe(processor);
		verify(processor, timeout(Duration.ofSeconds(3).toMillis())).cancel();
	}
}
