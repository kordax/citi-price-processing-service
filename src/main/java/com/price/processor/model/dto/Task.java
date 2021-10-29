package com.price.processor.model.dto;

import com.price.processor.model.CcyPair;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Task {
	private CcyPair pair;
	private CompletableFuture<Boolean> completableFuture;
	private Long startTime;
}
