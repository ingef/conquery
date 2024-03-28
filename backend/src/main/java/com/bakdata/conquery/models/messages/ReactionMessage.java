package com.bakdata.conquery.models.messages;

import java.util.UUID;

import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.bakdata.conquery.models.messages.namespaces.ActionReactionMessage;
import com.bakdata.conquery.models.messages.namespaces.NamespaceMessage;
import com.bakdata.conquery.models.worker.WorkerHandler;

/**
 * Interface for a {@link NamespaceMessage} implementing {@link ActionReactionMessage} to notify the {@link WorkerHandler} to checkoff the processed reaction of this message.
 */
public interface ReactionMessage {

	UUID getCallerId();

	WorkerId getWorkerId();

	boolean lastMessageFromWorker();
}
