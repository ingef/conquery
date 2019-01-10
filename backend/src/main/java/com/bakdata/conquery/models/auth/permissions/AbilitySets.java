package com.bakdata.conquery.models.auth.permissions;
import java.util.EnumSet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AbilitySets {
	QUERY_EXECUTOR(EnumSet.of(
			Ability.READ,
			Ability.CANCEL)),
	QUERY_CREATOR(EnumSet.of(
			Ability.READ,
			Ability.DELETE,
			Ability.SHARE,
			Ability.TAG,
			Ability.CANCEL,
			Ability.LABEL));

	private final EnumSet<Ability> set;
}
