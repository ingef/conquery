package com.bakdata.conquery.models.messages.namespaces;

import java.util.UUID;

import com.bakdata.conquery.models.messages.ReactionMessage;
import com.bakdata.conquery.models.worker.WorkerHandler;

/**
 * Interface for {@link WorkerMessage}s that require postprocessing on the manager, after all workers responded with possibly multiple {@link ReactionMessage} that are not final and a single {@link com.bakdata.conquery.models.messages.namespaces.specific.FinalizeReactionMessage}.
 */
public interface ActionReactionMessage {

	/**
	 * This id is used to keep track of the reaction.
	 * <ol>
	 *     <li>Upon sending this message {@link WorkerHandler} registers the message id</li>
	 *     <li>The react method of a {@link ActionReactionMessage} creates a {@link ReactionMessage} which carries this id</li>
	 *     <li>After processing of an {@link ReactionMessage} the {@link WorkerHandler} checks off reactions from each worker</li>
	 *     <li>When all workers checked ofd the afterAllReaction is executed</li>
	 * </ol>relayed by the {@link ReactionMessage} back to the
	 */
	UUID getMessageId();

	/**
	 * This hook is called after all expected {@link ReactionMessage}s were received.
	 */
	void afterAllReaction();
}
