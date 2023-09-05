package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Collection;

import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import lombok.Data;

@Data
public class RegisterColumnValues extends NamespaceMessage {
	@NsIdRef
	private final Column column;

	private final Collection<String> values;

	@Override
	public void react(DistributedNamespace context) throws Exception {
		context.addColumnValues(column, values);

	}
}
