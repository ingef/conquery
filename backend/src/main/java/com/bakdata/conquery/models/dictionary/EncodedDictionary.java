package com.bakdata.conquery.models.dictionary;

import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;

/**
 * Handle class allowing direct encoded access to Dictionary without wrapping inside StringTypeEncoded.
 *
 * Main usage is PrimaryDictionary.
 */
public class EncodedDictionary {

	private final Dictionary dict;
	private final StringTypeEncoded.Encoding encoding;

	public EncodedDictionary(Dictionary dict, StringTypeEncoded.Encoding encoding) {
		this.dict = dict;
		this.encoding = encoding;
	}

	public String getElement(int id) {
		return encoding.encode(dict.getElement(id));
	}

	public int getId(String value) {
		return dict.getId(encoding.decode(value));
	}

	public int getSize() {
		return dict.size();
	}
}
