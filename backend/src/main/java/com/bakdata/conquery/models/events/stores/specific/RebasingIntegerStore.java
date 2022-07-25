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
public class RebasingIntegerStore implements IntegerStore {

	private final long min;

	private final long root;

	private final IntegerStore store;

	public RebasingIntegerStore(long min, long root, IntegerStore store) {
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
	public RebasingIntegerStore select(int[] starts, int[] length) {
		return new RebasingIntegerStore(min, root, store.select(starts, length));
	}

	@Override
	public void setInteger(int event, @Nullable long value) {
		store.setInteger(event, value - min + root);
	}

	@Override
	public @NotNull long getInteger(int event) {
		return -root + min + store.getInteger(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public void setNull(int event) {
		store.setNull(event);
	}
}
