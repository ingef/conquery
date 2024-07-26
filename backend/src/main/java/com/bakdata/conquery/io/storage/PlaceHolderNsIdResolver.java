package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@ToString(onlyExplicitlyIncluded = true)
public class PlaceHolderNsIdResolver implements NsIdResolver {

	public final static PlaceHolderNsIdResolver DEFAULT_INSTANCE = new PlaceHolderNsIdResolver("default-instance");
	public final static PlaceHolderNsIdResolver TEST_INSTANCE = new PlaceHolderNsIdResolver("test-instance");

	public static final String ERROR_MSG = "Cannot be used in this environment. This id '%s' cannot be resolved on this node.";

	@ToString.Include
	private final String label;

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}

	@Override
	public <ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id) {
		throw new UnsupportedOperationException(ERROR_MSG.formatted(id));
	}
}
