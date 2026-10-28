package com.bakdata.conquery.models.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartupConfig {

	private boolean updateMatchingStats = false;
	private boolean updateFilterSearch = false;
}
