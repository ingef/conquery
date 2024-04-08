package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.ReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * This message returns the result of the {@link CollectColumnValuesJob} to the namespace on the manager.
 */
@CPSType(id = "REGISTER_COLUMN_VALUES", base = NamespacedMessage.class)
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Slf4j
@ToString
public class RegisterColumnValues extends NamespaceMessage implements ReactionMessage {

	private UUID callerId;

	private WorkerId workerId;

	@NsIdRef
	private final Column column;
	private final Collection<String> values;

	@Override
	public void react(DistributedNamespace context) throws Exception {
		if (log.isTraceEnabled()) {
			log.trace("Registering values for column '{}': {}", column.getId(), Arrays.toString(values.toArray()));
		}
		else {
			log.debug("Registering {} values for column '{}'", values.size(), column.getId());
		}

		context.getFilterSearch().registerValues(column, values);
	}

	@Override
	public boolean lastMessageFromWorker() {
		return false;
	}
}
