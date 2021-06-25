package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@CPSType(id="REMOVE_CONCEPT", base=NamespacedMessage.class)
@RequiredArgsConstructor(onConstructor_=@JsonCreator) @Getter @ToString
public class RemoveConcept extends WorkerMessage.Slow {

	@NsIdRef
	private final Concept<?> concept;
	
	@Override
	public void react(Worker context) throws Exception {
		synchronized (context.getStorage()) {
			context.removeConcept(concept);
		}
	}
}
