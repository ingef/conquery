package com.bakdata.conquery.models.index;

import java.net.URL;

import lombok.Data;

/**
 * Abstract base class for {@link IndexKey} which is used by {@link IndexService} to create and cache a new {@link Index}.
 *
 * @param <I> The type of Index that is indexed by this key
 */
@Data
public abstract class AbstractIndexKey<I extends Index<? extends IndexKey<I>>> implements IndexKey<I> {
	private final URL csv;
	private final String internalColumn;
}
