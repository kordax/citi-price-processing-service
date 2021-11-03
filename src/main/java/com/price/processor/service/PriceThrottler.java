package com.price.processor.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.price.processor.config.PriceThrottlerConfig;
import com.price.processor.event.ExchangeRatesChangedEvent;
import com.price.processor.exception.ApplicationErrorException;
import com.price.processor.model.dto.Task;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * {@link PriceProcessor} implementation matching the task description:
 * <br>
 * You have to write a PriceThrottler class which will implement the following requirements: 1) Implement PriceProcessor
 * interface 2) Distribute updates to its listeners which are added through subscribe() and removed through
 * unsubscribe() 3) Some subscribers are very fast (i.e. onPrice() for them will only take a microsecond) and some are
 * very slow (onPrice() might take 30 minutes). Imagine that some subscribers might be showing a price on a screen and
 * some might be printing them on a paper 4) Some ccyPairs change rates 100 times a second and some only once or two
 * times a day 5) ONLY LAST PRICE for each ccyPair matters for subscribers. I.e. if a slow subscriber is not coping with
 * updates for EURUSD - it is only important to deliver the latest rate 6) It is important not to miss rarely changing
 * prices. I.e. it is important to deliver EURRUB if it ticks once per day but you may skip some EURUSD ticking every
 * second 7) You don't know in advance which ccyPair are frequent and which are rare. Some might become more frequent at
 * different time of a day 8) You don't know in advance which of the subscribers are slow and which are fast. 9) Slow
 * subscribers should not impact fast subscribers
 * <p>
 * In short words the purpose of PriceThrottler is to solve for slow consumers
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PriceThrottler implements PriceProcessor, ApplicationListener<ExchangeRatesChangedEvent> {

	private final PriceThrottlerConfig config;
	private final ExchangeRatesMonitor monitor;

	// I wanted to use a PriorityQueue here, but there's no info in the PriceProcessor at all and we cannot sort it by the definition
	@Getter
	private List<PriceProcessor> subscribers = new LinkedList<>();
	// That's a workaround because we cannot change the PriceProcessor class at all and
	// interface is already defined by the task definition
	@Getter
	private Map<PriceProcessor, UUID> subscriberIds = new HashMap<>();

	// Processor with it's running task that returns a future with operation start time
	@Getter
	private Multimap<PriceProcessor, Task> runningTasks = HashMultimap.create();

	private ExecutorService threadPool;

	@PostConstruct
	public void init() {
		threadPool = Executors.newFixedThreadPool(config.getMaxSubscribers());
	}

	public UUID getProcessorUUID(PriceProcessor processor) {
		return subscriberIds.get(processor);
	}

	@Nullable
	public PriceProcessor getProcessor(UUID uuid) {
		return subscriberIds.entrySet()
			.stream()
			.filter(e -> e.getValue().equals(uuid))
			.map(Entry::getKey)
			.findFirst().orElse(null);
	}

	/**
	 * Call from an upstream
	 * <p>
	 * You can assume that only correct data comes in here - no need for extra validation
	 *
	 * @param ccyPair - EURUSD, EURRUB, USDJPY - up to 200 different currency pairs
	 * @param rate    - any double rate like 1.12, 200.23 etc
	 */
	@Override
	public void onPrice(String ccyPair, double rate) {
		log.info("onPrice called, {} subscribers, ccyPair '{}', rate '{}'", subscribers.size(), ccyPair, rate);
		subscribers.forEach(subscriber -> {
			if (runningTasks.containsKey(subscriber)) {
				if (runningTasks.get(subscriber).stream().anyMatch(t -> t.getPair().toString().equals(ccyPair))) {
					try {
						handleAlreadyRunningTask(subscriber, ccyPair, rate);
					} catch (ApplicationErrorException e) {
						log.error("Error on handling already running task", e);
					}
				}
			} else {
				try {
					sendTask(subscriber, ccyPair, rate);
				} catch (ApplicationErrorException e) {
					log.error("onPrice error", e);
				}
			}
		});
	}

	/**
	 * Subscribe for updates
	 * <p>
	 * Called rarely during operation of PriceProcessor
	 *
	 * @param priceProcessor - can be up to 200 subscribers
	 */
	@Override
	public void subscribe(PriceProcessor priceProcessor) {
		if (subscribers.size() >= config.getMaxSubscribers()) {
			log.error("Subscribers limit of {} has been reached", config.getMaxSubscribers());

			return;
		}

		final UUID uuid = UUID.randomUUID();
		log.info("Subscribing processor with UUID: {}", uuid);

		subscriberIds.put(priceProcessor, uuid);
		subscribers.add(priceProcessor);
	}

	/**
	 * Unsubscribe from updates
	 * <p>
	 * Called rarely during operation of PriceProcessor
	 *
	 * @param priceProcessor price processor instance
	 */
	@Override
	public void unsubscribe(PriceProcessor priceProcessor) {
		if (!subscriberIds.containsKey(priceProcessor)) {
			log.error("Processor is not in the subscribers list");

			return;
		}

		final UUID uuid = subscriberIds.get(priceProcessor);
		log.info("Unsubscribing processor with UUID: {}", uuid);

		subscribers.remove(priceProcessor);
		subscriberIds.remove(priceProcessor);
	}

	@Override
	public boolean cancel() {
		return subscribers.stream().allMatch(PriceProcessor::cancel);
	}

	private void sendTask(PriceProcessor subscriber, String ccyPair, double rate) throws ApplicationErrorException {
		final CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(
			() -> {
				try {
					subscriber.onPrice(ccyPair, rate);
					return true;
				} catch (Exception e) {
					return false;
				}
			},
			threadPool
		).thenApply((Boolean result) -> {
			final Task task = runningTasks.get(subscriber)
				.stream()
				.filter(t -> t.getPair().toString().equals(ccyPair))
				.findFirst().orElseThrow();
			runningTasks.remove(subscriber, task);

			return result;
		});

		Task task = new Task(CurrencyUtil.codePairToCurrPair(ccyPair), future, System.currentTimeMillis());

		runningTasks.put(subscriber, task);
	}

	private void handleAlreadyRunningTask(PriceProcessor subscriber, String ccyPair, double rate)
		throws ApplicationErrorException {
		final UUID uuid = subscriberIds.get(subscriber);
		final Task task = runningTasks.get(subscriber)
			.stream()
			.filter(t -> t.getPair().toString().equals(ccyPair))
			.findFirst()
			.orElseThrow();
		if (monitor.isRare(CurrencyUtil.codePairToCurrPair(ccyPair))) {
			log.info("Received a rare exchange rate task: {}", uuid);
			subscriber.cancel();
			task.getCompletableFuture().cancel(false);
			sendTask(subscriber, ccyPair, rate);
		} else {
			if (task.getStartTime() > config.getSoftTimeout().toMillis()) {
				log.info("Skipping a non-rare exchange rate change");
				onSkip();
			}
		}
	}

	@Override
	public void onApplicationEvent(ExchangeRatesChangedEvent event) {
		event.getRates().forEach(
			(pair, rate) -> onPrice(pair.toString(), rate)
		);
	}

	public void onSkip() {

	}
}
