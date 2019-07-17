package com.bakdata.conquery.models.api.description;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FEValidityDate {
	private String defaultValue;
	private List<FEValue> options;
}
