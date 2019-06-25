package com.bakdata.conquery.models.dictionary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class DictionaryEntry {
	private int id;
	private byte[] value;
}