package com.bakdata.conquery.sql.conversion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SharedAliases {

	PRIMARY_COLUMN("primary_id"),
	SECONDARY_ID("secondary_id"),
	DATES_COLUMN("dates");

	private final String alias;
}
