package com.bakdata.conquery.models.error;

import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Base class, that is intended for external serialization, without type information.
 */
@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class SimpleErrorInfo implements ConqueryErrorInfo {
	@NotNull
	private final UUID id;
	private final String code;
	private final String message;

	@Override
	public SimpleErrorInfo asPlain() {
		return this;
	}
}
