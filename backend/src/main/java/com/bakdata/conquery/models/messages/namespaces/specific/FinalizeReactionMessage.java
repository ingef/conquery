package com.bakdata.conquery.models.messages.namespaces.specific;

import java.util.UUID;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.ReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespacedMessage;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;


/**
 * Use {@link ActionReactionMessage#afterAllReaction()} to processing on initiator side after all reactions where collected.
 */
@CPSType(id = "FINALIZE_REACTION_MESSAGE", base = NamespacedMessage.class)
@AllArgsConstructor(onConstructor_ = @JsonCreator)
@Getter
@Slf4j
@ToString
public final class FinalizeReactionMessage extends NamespaceMessage implements ReactionMessage {

	private UUID callerId;

	private WorkerId workerId;

	@Override
	public boolean lastMessageFromWorker() {
		return true;
	}

	@Override
	public void react(DistributedNamespace context) throws Exception {
		log.debug("Received finalize message from caller '{}' workerId '{}'", callerId, workerId);
	}
}
