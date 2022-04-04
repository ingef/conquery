package com.bakdata.conquery.models.dictionary;

import lombok.Value;

@Value
public class DictionaryEntry {
	private final int id;
	private final byte[] value;
}