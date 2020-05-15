package com.bakdata.conquery.metrics;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
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

@Slf4j
@PreMatching
// Chain this filter after the Authentication filter
@Priority(2000)
@RequiredArgsConstructor
public class ActiveUsersFilter implements ContainerRequestFilter {

	private static final String USERS = "users";
	private static final String ACTIVE = "active";

	private final MasterMetaStorage storage;

	private final Table<Group, User, LocalDateTime> activeUsers = HashBasedTable.create();

	@Override
	public void filter(ContainerRequestContext requestContext) {
		final Principal userPrincipal = requestContext.getSecurityContext().getUserPrincipal();

		if(!(userPrincipal instanceof User)){
			return;
		}

		final User user = (User) userPrincipal;
		final Optional<Group> groupOptional = AuthorizationHelper.getPrimaryGroup(user, storage);

		if (groupOptional.isEmpty()) {
			log.debug("{} has no primary group", user);
			return;
		}

		final Group group = groupOptional.get();

		activeUsers.put(group, user, LocalDateTime.now());

		final String metricName = MetricRegistry.name(USERS, group.getName(), ACTIVE);

		// No need to register the gauge multiple times.
		if(!SharedMetricRegistries.getDefault().getGauges(((name, metric) -> metricName.equalsIgnoreCase(name))).isEmpty())
			return;

		SharedMetricRegistries.getDefault().gauge(metricName, () -> createActiveUsersGauge(group));
	}

	private Gauge<Integer> createActiveUsersGauge(Group group){
		return () -> {
			int active = 0;
			for (Map.Entry<User, LocalDateTime> usageTimes : activeUsers.row(group).entrySet()) {
				if(usageTimes.getValue().isBefore(LocalDateTime.now().minusHours(ConqueryConfig.getInstance().getMetricsConfig().getUserActiveHours()))){
					continue;
				}
				active++;
			}
			return active;
		};
	}
}
