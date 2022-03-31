package com.bakdata.conquery.models.dictionary;

/**
 * Handle class allowing direct encoded access to Dictionary without wrapping inside StringTypeEncoded.
 * <p>
 * Main usage is PrimaryDictionary.
 */
public class EncodedDictionary {

	private final Dictionary dict;

	public EncodedDictionary(Dictionary dict) {
		this.dict = dict;
	}

	public String getElement(int id) {
		return dict.getElement(id);
	}

	public int getId(String value) {
		return dict.getId(value);
	}

	public int getSize() {
		return dict.size();
	}
}
