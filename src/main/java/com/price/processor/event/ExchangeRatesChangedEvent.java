package com.price.processor.event;

import com.price.processor.model.CcyPair;
import java.util.Map;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter

public class ExchangeRatesChangedEvent extends ApplicationEvent {
	private final Map<CcyPair, Double> rates;

	public ExchangeRatesChangedEvent(Object source, Map<CcyPair, Double> rates) {
		super(source);
		this.rates = rates;
	}
}
