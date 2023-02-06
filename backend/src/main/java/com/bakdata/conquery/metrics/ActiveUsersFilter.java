package com.bakdata.conquery.metrics;

import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * According to https://download.oracle.com/javaee-archive/jersey.java.net/users/2009/08/7146.html and other sources
 * filters are singletons.
 */
@Slf4j
@PreMatching
// Chain this filter after the Authentication filter
@Priority(2000)
public class ActiveUsersFilter implements ContainerRequestFilter {

	private static final String USERS = "users";
	private static final String ACTIVE = "active";

	@Inject
	private MetaStorage storage;
	@Inject
	private ConqueryConfig config;

	private Duration activeUserDuration = Duration.ofMinutes(30);

	@PostConstruct
	void init() {
		activeUserDuration = Duration.ofMinutes(config.getMetricsConfig().getUserActiveDuration().toMinutes());
	}

	private final Table<Group, User, LocalDateTime> activeUsers = HashBasedTable.create();

	@Override
	public void filter(ContainerRequestContext requestContext) {
		final Principal userPrincipal = requestContext.getSecurityContext().getUserPrincipal();

		if (!(userPrincipal instanceof User)) {
			return;
		}

		final User user = (User) userPrincipal;
		final Optional<Group> groupOptional = AuthorizationHelper.getPrimaryGroup(user, storage);

		if (groupOptional.isEmpty()) {
			log.trace("{} has no primary group", user);
			return;
		}

		final Group group = groupOptional.get();

		activeUsers.put(group, user, LocalDateTime.now());

		final String metricName = MetricRegistry.name(USERS, group.getName(), ACTIVE);

		// This does not register multiple gauges, but reuses them under the hood.
		SharedMetricRegistries.getDefault().gauge(metricName, () -> activeUsersGauge(group));
	}

	/**
	 * Count the number of users who have issued a request in the configured duration.
	 */
	private Gauge<Integer> activeUsersGauge(Group group){
		return () -> {
			int active = 0;
			for (Map.Entry<User, LocalDateTime> usageTimes : activeUsers.row(group).entrySet()) {
				if(usageTimes.getValue().isBefore(LocalDateTime.now().minus(activeUserDuration))){
					continue;
				}

				active++;
			}
			return active;
		};
	}
}
