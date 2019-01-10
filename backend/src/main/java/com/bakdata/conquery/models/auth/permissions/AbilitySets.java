package com.bakdata.conquery.models.auth.permissions;
import java.util.EnumSet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AbilitySets {
	READER(EnumSet.of(Ability.READ)),
	CREATOR(EnumSet.of(Ability.READ, Ability.DELETE, Ability.SHARE));

	private final EnumSet<Ability> set;
}
