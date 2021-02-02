package com.bakdata.conquery.models.events.stores.specific;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

/**
 * Delegates to an underlying Store to transform values into a smaller bit-size.
 */
@CPSType(base = ColumnStore.class, id = "REBASE")
@Getter
@ToString(of = {"min", "store"})
public class RebasingStore implements IntegerStore {

	private final long min;

	private final long root;

	private final IntegerStore store;

	public RebasingStore(long min, long root, IntegerStore store) {
		this.min = min;
		this.root = root;
		this.store = store;
	}

	@Override
	public int getLines() {
		return store.getLines();
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public RebasingStore select(int[] starts, int[] length) {
		return new RebasingStore(min, root, store.select(starts, length));
	}

	@Override
	public void set(int event, @Nullable Object value) {
		if (value == null) {
			store.set(event, null);
			return;
		}

		store.set(event, ((Number) value).longValue() - min + root);
	}

	@Override
	public Long get(int event) {
		return getInteger(event);
	}

	@Override
	public @NotNull long getInteger(int event) {
		return - root + min + store.getInteger(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
