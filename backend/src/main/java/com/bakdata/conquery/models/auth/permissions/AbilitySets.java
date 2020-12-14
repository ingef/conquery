package com.bakdata.conquery.models.auth.permissions;

import java.util.EnumSet;

import com.bakdata.conquery.io.cps.CPSBase;

@CPSBase
public class AbilitySets {

	public static final EnumSet<Ability> QUERY_EXECUTOR = EnumSet.of(Ability.READ, Ability.CANCEL);
	public static final EnumSet<Ability> QUERY_CREATOR = EnumSet.of(
		Ability.READ, 
		Ability.DELETE, 
		Ability.SHARE, 
		Ability.TAG, 
		Ability.CANCEL, 
		Ability.LABEL);
	public static final EnumSet<Ability> FORM_CONFIG_CREATOR = EnumSet.of(
		Ability.READ,
		Ability.DELETE,
		Ability.SHARE,
		Ability.TAG,
		Ability.LABEL,
		Ability.MODIFY);
	public static final EnumSet<Ability> SHAREHOLDER = EnumSet.of(
		Ability.READ);
	public static final EnumSet<Ability> DATASET_CREATOR = EnumSet.of(Ability.READ, Ability.DOWNLOAD, Ability.PRESERVE_ID);
}
