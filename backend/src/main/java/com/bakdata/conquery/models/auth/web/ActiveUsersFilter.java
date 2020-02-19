package com.bakdata.conquery.models.auth.web;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@PreMatching
// Chain this filter after the Authentication filter
@Priority(2000)
@RequiredArgsConstructor
public class ActiveUsersFilter implements ContainerRequestFilter {

	private final MasterMetaStorage storage;

	/**
	 * Google cache managing evicition etc for us.
	 */
	private final Cache<User, Boolean> activeUsers = CacheBuilder.newBuilder()
														   .expireAfterAccess(ConqueryConfig.getInstance().getMetricsConfig().getUserActiveHours(), TimeUnit.HOURS)
															.removalListener(notification -> decrementPrimaryGroupCount((User) notification.getKey()))
															.build();




	public void incrementPrimaryGroupCount(User user) {
		final Group primaryGroup = AuthorizationHelper.getPrimaryGroup(user, storage);

		// Groups with less than three users are not tracked, for privacy reasons.
		if(primaryGroup.getMembers().size() <= ConqueryConfig.getInstance().getMetricsConfig().getGroupTrackingMinSize())
			return;

		SharedMetricRegistries.getDefault().counter(primaryGroup.getName() + ".active").inc();
	}

	public void decrementPrimaryGroupCount(User user) {
		final Group primaryGroup = AuthorizationHelper.getPrimaryGroup(user, storage);

		// Groups with less than three users are not tracked.
		if(primaryGroup.getMembers().size() <= ConqueryConfig.getInstance().getMetricsConfig().getGroupTrackingMinSize())
			return;

		SharedMetricRegistries.getDefault().counter(primaryGroup.getName() + ".active").dec();
	}


	@SneakyThrows
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		final Principal userPrincipal = requestContext.getSecurityContext().getUserPrincipal();

		if(userPrincipal == null){
			return;
		}

		activeUsers.get(((User) userPrincipal), () -> {
			incrementPrimaryGroupCount((User) userPrincipal);
			return true;
		});
	}
}
