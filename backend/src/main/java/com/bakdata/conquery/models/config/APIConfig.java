package com.bakdata.conquery.models.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class APIConfig {

	private boolean allowCORSRequests = false;
	private boolean caching = true;
}