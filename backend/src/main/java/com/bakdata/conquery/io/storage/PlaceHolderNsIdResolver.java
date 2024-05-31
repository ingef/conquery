package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceHolderNsIdResolver implements NsIdResolver {

	public final static PlaceHolderNsIdResolver INSTANCE = new PlaceHolderNsIdResolver();
	public static final String ERROR_MSG = "Cannot be used in this environment. This id cannot be resolved on this node.";

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(NsIdResolver.class, this);
	}

	@Override
	public <ID extends Id<?> & NamespacedId, VALUE> VALUE get(ID id) {
		throw new UnsupportedOperationException(ERROR_MSG);
	}
}
