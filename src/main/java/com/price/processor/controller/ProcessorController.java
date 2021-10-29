package com.price.processor.controller;

import com.price.processor.service.DummyPriceProcessor;
import com.price.processor.service.PriceProcessor;
import com.price.processor.service.PriceThrottler;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/processor")
@RequiredArgsConstructor
@Slf4j
public class ProcessorController {
	private final PriceThrottler throttler;

	@PutMapping("subscribe")
	public String subscribeProcessor(@RequestParam Long operationTimeMs) {
		log.info("Subscribing processor with operationTimeMs {}", operationTimeMs);
		PriceProcessor processor = new DummyPriceProcessor(operationTimeMs);

		throttler.subscribe(processor);
		return throttler.getProcessorUUID(processor).toString();
	}

	@DeleteMapping("unsubscribe")
	public void unsubscribeProcessor(@RequestParam String uuid) {
		log.info("Unsubscribing processor with uuid {}", uuid);
		PriceProcessor processor = throttler.getProcessor(UUID.fromString(uuid));
		throttler.unsubscribe(processor);
	}
}
