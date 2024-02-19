package com.bakdata.conquery.sql.conversion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public enum SharedAliases {

	PRIMARY_COLUMN("pid");

	private final String alias;
}
