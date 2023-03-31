package com.bakdata.conquery.apiv1.frontend;

import java.util.List;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FrontendValidityDate {

	/**
	 * Further information about the available validity dates.
	 */
	@Nullable
	private String tooltip;

	private String defaultValue;
	private List<FrontendValue> options;
}
