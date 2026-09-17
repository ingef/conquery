package com.bakdata.conquery.resources.admin;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

@Slf4j
public class ShutdownTask extends Task implements ServerLifecycleListener {

	private Server server;

	public ShutdownTask() {
		super("shutdown");
	}

	@Override
	public void serverStarted(Server server) {
		this.server = server;
	}

	@Override
	public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
		log.info("Received Shutdown command");

		if(server == null) {
			output.print("Server not yet started");
			return;
		}

		output.print("Shutting down");
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
