package com.bakdata.conquery.apiv1.frontend;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FrontendValidityDate {
	private String defaultValue;
	private List<FrontendValue> options;
}
