package com.bakdata.conquery.models.error;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Base class, that is intended for external serialization, without type information.
 */
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class PlainError implements ConqueryErrorInfo {
	private final UUID id;
	private final String code;
	private String message;
	private Map<String,String> context;
	
	@Override
	public PlainError asPlain() {
		return this;
	}
}
