package com.bakdata.conquery.models.messages.namespaces.specific;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.messages.namespaces.WorkerMessage;
import com.bakdata.conquery.models.worker.Worker;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@CPSType(id="UPDATE_SECONDARYID", base=NamespacedMessage.class) @Slf4j
@AllArgsConstructor(onConstructor_=@JsonCreator) @Getter @Setter @ToString(callSuper=true)
public class UpdateSecondaryId extends WorkerMessage {

	private SecondaryIdDescription secondaryId;

	@Override
	public void react(Worker context) throws Exception {
		log.info("Received update of SecondaryId {}", secondaryId.getId());
		synchronized (context.getStorage()) {
			context.addSecondaryId(secondaryId);
		}
	}
}
