package com.price.processor.config;

import java.time.Duration;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "com.price.processor.generator")
@Data
@Validated
public class GeneratorConfig {
	@Positive
	@Max(100)
	private Double chance;

	@NotNull
	private Duration linger;
}
