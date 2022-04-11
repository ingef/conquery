package com.bakdata.conquery.models.config;

import javax.annotation.Nullable;
import javax.validation.constraints.Min;

import lombok.Data;

@Data
public class SearchConfig {
	@Min(0)
	private int suffixLength = 2;
	@Nullable
	private String split = "(),;.:\"'/";
}
