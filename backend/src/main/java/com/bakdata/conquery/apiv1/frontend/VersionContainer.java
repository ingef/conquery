package com.bakdata.conquery.apiv1.frontend;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.NonNull;

public record VersionContainer(
		@NonNull String name,
		String version,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
		Date buildTime
) {
}
