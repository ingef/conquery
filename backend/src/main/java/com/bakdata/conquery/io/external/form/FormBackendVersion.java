package com.bakdata.conquery.io.external.form;

import java.time.ZonedDateTime;


public record FormBackendVersion(
		String version,
		ZonedDateTime buildTime
) {
}
