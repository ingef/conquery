package com.bakdata.conquery.models.events.stores;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.types.ColumnStore;
import lombok.Getter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;


@CPSType(base = ColumnStore.class, id = "REBASE")
@Getter
@ToString(of = {"min", "store"})
public class RebasingStore extends ColumnStore<Long> {

	private final long min;

	private final long root;

	private final ColumnStore<Long> store;

	public RebasingStore(long min, long root, ColumnStore<Long> store) {
		super(store.getTypeId());
		this.min = min;
		this.root = root;
		this.store = store;
	}

	@Override
	public long estimateEventBytes() {
		return store.estimateEventBytes();
	}

	@Override
	public RebasingStore select(int[] starts, int[] length) {
		return new RebasingStore(min, root, store.select(starts, length));
	}

	@Override
	public void set(int event, @Nullable Long value) {
		if (value == null) {
			store.set(event, null);
			return;
		}

		store.set(event, value - min + root);
	}

	@Override
	public @NotNull Long get(int event) {
		return - root + min + store.get(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}
}
