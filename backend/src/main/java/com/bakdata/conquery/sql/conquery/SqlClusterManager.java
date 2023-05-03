package com.bakdata.conquery.sql.conquery;

import com.bakdata.conquery.models.worker.ClusterManager;
import org.apache.mina.core.session.IoSession;

/**
 * {@link ClusterManager} for ConQuery configured with the SQL connector backend.
 *
 * <p>
 * The SQL connector doesn't use shards, therefore a no-op implementation.
 */
public class SqlClusterManager implements ClusterManager {

	@Override
	public void sessionOpened(IoSession session) {
		// do nothing
	}

	@Override
	public void sessionClosed(IoSession session) {
		// do nothing
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
		// do nothing
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		// do nothing
	}

	@Override
	public void start() throws Exception {
		// do nothing
	}

	@Override
	public void stop() throws Exception {
		// do nothing
	}
}
