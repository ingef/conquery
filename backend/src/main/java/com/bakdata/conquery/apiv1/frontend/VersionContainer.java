package com.bakdata.conquery.apiv1.frontend;

import java.time.ZonedDateTime;

import lombok.NonNull;

public record VersionContainer(
		@NonNull String name,
		String version,
		ZonedDateTime buildTime
) {
}
