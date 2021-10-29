package com.price.processor.config;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "com.price.processor.exchange-rates")
@Data
public class ExchangeRatesConfig {
	private Duration rareChangingThreshold;
}
