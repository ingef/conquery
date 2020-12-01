package com.bakdata.conquery.models.events.stores;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.ColumnStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;


@CPSType(base = ColumnStore.class, id = "REBASE")
@RequiredArgsConstructor(onConstructor_ = {@JsonCreator})
@Getter
@ToString(of = {"min", "store"})
public class RebasingStore extends ColumnStoreAdapter<Long> {

	private final long min;

	private final long root;

	private final ColumnStore<Long> store;

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
