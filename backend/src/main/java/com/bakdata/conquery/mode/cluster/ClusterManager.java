package com.bakdata.conquery.mode.cluster;

import com.bakdata.conquery.mode.DelegateManager;
import com.bakdata.conquery.mode.Manager;
import com.bakdata.conquery.models.worker.DistributedNamespace;
import io.dropwizard.lifecycle.Managed;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * {@link Manager} for running ConQuery in cluster mode.
 */
@RequiredArgsConstructor
public class ClusterManager implements Manager {
	@Delegate(excludes = Managed.class)
	private final DelegateManager<DistributedNamespace> delegate;
	@Getter
	private final ClusterConnectionManager connectionManager;

	@Override
	public void start() throws Exception {
		delegate.start();
		connectionManager.start();
	}

	@Override
	public void stop() throws Exception {
		delegate.stop();
		connectionManager.stop();
	}

}
