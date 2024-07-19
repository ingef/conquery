package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.models.identifiable.ids.Id;

public interface MetaIdResolver {

	<ID extends Id<?>, VALUE> VALUE get(ID id);


}
