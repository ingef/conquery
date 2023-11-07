package com.bakdata.conquery.apiv1;

import lombok.Data;

@Data
public abstract class ResultColumnStatistics {
	private final String name;
	private final String label;
	private final String description;
	private final String type;

	public ResultColumnStatistics(String name, String label, String description, String type) {
		this.name = name;
		this.label = label;
		this.description = description;
		this.type = type;
	}
}
