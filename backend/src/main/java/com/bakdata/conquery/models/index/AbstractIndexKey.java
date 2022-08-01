package com.bakdata.conquery.models.index;

import java.net.URL;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@EqualsAndHashCode
public abstract class AbstractIndexKey<I extends Index<? extends IndexKey<I,V>,V>,V> implements IndexKey<I,V> {
	private final URL csv;
	private final String internalColumn;
	private final String externalTemplate;
}
