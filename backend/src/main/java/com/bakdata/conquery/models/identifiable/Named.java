package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.base.CharMatcher;

public interface Named<ID extends IId<? extends Identifiable<? extends ID>>> extends Identifiable<ID> {

	public static final CharMatcher INVALID_CHARACTERS = CharMatcher.is(IId.JOIN_CHAR).or(CharMatcher.whitespace());
	public static final CharMatcher VALID_CHARACTERS = INVALID_CHARACTERS.negate();
	
	String getName();
	
	public static String makeSafe(String name) {
		if(name == null || name.isEmpty()) {
			throw new IllegalArgumentException("I can't make '"+name+"' a safe name");
		}
		return INVALID_CHARACTERS.replaceFrom(name.toLowerCase(),'_');
	}
}
