package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.shiro.authz.Permission;

/**
 * Dropwizard Task deleting queries that are not used anymore. Defined as:
 * - not named
 * - older than 30 days
 * - is not shared
 * - no tags
 * - not referenced by other queries
 */
@Slf4j
public class QueryCleanupTask extends Task {

	MasterMetaStorage storage;

	public QueryCleanupTask(MasterMetaStorage storage) {
		super("query-cleanup");
		this.storage = storage;
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

		final Duration oldQueriesTime = ConqueryConfig.getInstance().getQueries().getOldQueriesTime();


		// Iterate for as long as no changes are needed (this is because queries can be referenced by other queries)
		while (true) {
			final QueryUtils.AllReusedFinder reusedChecker = new QueryUtils.AllReusedFinder();
			List<ManagedExecutionId> toDelete = new ArrayList<>();

			for (ManagedExecution<?> execution : storage.getAllExecutions()) {

				// Gather all referenced queries via reused checker.
				if (execution instanceof ManagedQuery) {
					((ManagedQuery) execution).getQuery().visit(reusedChecker);
				}
				else if (execution instanceof ManagedForm) {
					((ManagedForm) execution).getFlatSubQueries().values()
											 .forEach(q -> q.getQuery().visit(reusedChecker));
				}

				if (execution.isShared()) {
					continue;
				}
				log.trace("{} is not shared", execution.getId());


				if (ArrayUtils.isNotEmpty(execution.getTags())) {
					continue;
				}
				log.trace("{} has no tags", execution.getId());

				if (execution.getLabel() != null) {
					continue;
				}
				log.trace("{} has no label", execution.getId());

				if(execution.getCreationTime().until(LocalDateTime.now(), oldQueriesTime.getUnit().toChronoUnit()) < oldQueriesTime.getQuantity()) {
					continue;
				}
				log.trace("{} is not older than {}.", execution.getId(), oldQueriesTime);

				toDelete.add(execution.getId());
			}

			// remove all queries referenced in reused queries.
			toDelete.removeAll(
					reusedChecker.getReusedElements().stream()
								 .map(CQReusedQuery::getQuery)
								 .collect(Collectors.toList()));

			if (toDelete.isEmpty()) {
				break;
			}

			for (ManagedExecutionId managedExecutionId : toDelete) {
				log.debug("Deleting now unused Execution `{}`", managedExecutionId);
				storage.removeExecution(managedExecutionId);
			}
			
			// Iterate over all PermissionOwners
			
			log.info("Permissions deleted from all users: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllUsers()));
			log.info("Permissions deleted from all groups: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllGroups()));
			log.info("Permissions deleted from all roles: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllRoles()));
		}
	}

	/**
	 * Deletes permission that reference non-existing executions.
	 * @return The number of deleted permissions.
	 */
	public static int deleteQueryPermissionsWithMissingRef(MasterMetaStorage storage, Iterable<? extends PermissionOwner<?>> owners) {
		int countDeleted = 0;
		// Do the loop-di-loop
		for(PermissionOwner<?> owner : owners) {
			for (Permission permission : owner.getPermissions()) {
				if(!(permission instanceof WildcardPermission)) {
					log.warn("Encountered the permission type {} that is not handled by this routine. Permission was: {}", permission.getClass(), permission);
					continue;
				}
				WildcardPermission wpermission = (WildcardPermission) permission;
				if(!wpermission.getDomains().contains(QueryPermission.DOMAIN.toLowerCase())) {
					// Skip Permissions that do not reference an Execution/Query
					continue;
				}
				
				// Handle multiple references to instances
				Set<String> validRef = new HashSet<>();
				for(String sId : wpermission.getInstances()) {
					ManagedExecutionId mId = ManagedExecutionId.Parser.INSTANCE.parse(sId);
					if (storage.getExecution(mId) != null) {
						// Execution exists -- it is a valid reference
						validRef.add(mId.toString());
					}
				}
				if(validRef.size() > 0) {
					if (wpermission.getInstances().size() == validRef.size()) {
						// All are valid, nothing changed proceed with the next permission
						continue;
					}
					// Create a new Permission that only contains valid references
					WildcardPermission reducedPermission = new WildcardPermission(
						List.of(
							wpermission.getDomains(),  
							wpermission.getAbilities(), 
							validRef),
						wpermission.getCreationTime());
					owner.addPermission(storage, reducedPermission);					
				}
				
				// Delete the old permission that containes both valid and invalid references
				owner.removePermission(storage, wpermission);
				countDeleted++;
				
			}
		}
		return countDeleted;
	}
}
