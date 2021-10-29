package com.price.processor.model.dto.json;

import lombok.Data;

@Data
public class JsonExchEntry {
	private String pair;
	private Double bid;
	private Double ask;
}