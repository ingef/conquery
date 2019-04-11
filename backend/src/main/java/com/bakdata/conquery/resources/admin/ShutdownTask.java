package com.bakdata.conquery.resources.admin;

import java.io.PrintWriter;

import org.eclipse.jetty.server.Server;

import com.google.common.collect.ImmutableMultimap;

import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ShutdownTask extends Task implements ServerLifecycleListener {

	private Server server;

	public ShutdownTask() {
		super("shutdown");
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
		if(server == null) {
			output.print("Server not yet started");
		}
		else {
			output.print("Shutting down");
			log.info("Received Shutdown command");
			//this must be done in an extra step or the shutdown will wait for this request to be resolved
			new Thread("shutdown waiter thread") {
				@Override
				public void run() {
					try {
						server.stop();
					} catch (Exception e) {
						log.error("Failed while shutting down", e);
					}
				}
			}.start();
		}
	}

	@Override
	public void serverStarted(Server server) {
		this.server = server;
	}

}
