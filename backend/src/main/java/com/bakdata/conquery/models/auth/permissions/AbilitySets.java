package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;

public class AbilitySets {

	public final static EnumSet<Ability> QUERY_EXECUTOR = EnumSet
		.of(Ability.READ, Ability.CANCEL);
	public final static EnumSet<Ability> QUERY_CREATOR = EnumSet.of(
		Ability.READ,
		Ability.DELETE,
		Ability.SHARE,
		Ability.TAG,
		Ability.CANCEL,
		Ability.LABEL);
	public final static EnumSet<Ability> DATASET_CREATOR = EnumSet
		.of(Ability.READ, Ability.DELETE, Ability.DOWNLOAD);
}
