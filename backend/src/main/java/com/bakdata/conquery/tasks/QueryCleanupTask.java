package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.forms.managed.ManagedForm;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.query.concept.specific.CQReusedQuery;
import com.bakdata.conquery.util.QueryUtils;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
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

    public static final String EXPIRATION_PARAM = "expiration";
	private static final Predicate<String> UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$").asPredicate();

    private final MasterMetaStorage storage;
    private Duration queryExpiration;

    public QueryCleanupTask(MasterMetaStorage storage, Duration queryExpiration) {
		super("cleanup");
		this.storage = storage;
		this.queryExpiration = queryExpiration;
	}

	public static boolean isDefaultLabel(String label){
		return UUID_PATTERN.test(label);
	}

	@Override
	public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {

	    Duration queryExpiration = this.queryExpiration;

	    if(parameters.containsKey(EXPIRATION_PARAM)) {
	        if(parameters.get(EXPIRATION_PARAM).size() > 1){
	            log.warn("Will not respect more than one expiration time. Have `{}`",parameters.get(EXPIRATION_PARAM));
            }

            queryExpiration = Duration.parse(parameters.get(EXPIRATION_PARAM).asList().get(0));
        }

	    if(queryExpiration == null){
	        throw new IllegalArgumentException("Query Expiration may not be null");
        }

	    log.info("Starting deletion of queries older than {} of {}", queryExpiration, storage.getAllExecutions().size());

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

				if (execution.getLabel() != null || isDefaultLabel(execution.getLabel())) {
					continue;
				}
				log.trace("{} has no label", execution.getId());


				if (LocalDateTime.now().minus(queryExpiration).isBefore(execution.getCreationTime())) {
					continue;
				}
				log.trace("{} is not older than {}.", execution.getId(), queryExpiration);

				toDelete.add(execution.getId());
			}

			// remove all queries referenced in reused queries.
			toDelete.removeAll(
					reusedChecker.getReusedElements().stream()
								 .map(CQReusedQuery::getQuery)
								 .collect(Collectors.toList()));

			if (toDelete.isEmpty()) {
				log.info("No queries to delete");
				break;
			}

			log.info("Deleting {} Executions", toDelete.size());

			for (ManagedExecutionId managedExecutionId : toDelete) {
				log.trace("Deleting Execution[{}]", managedExecutionId);
				storage.removeExecution(managedExecutionId);
			}
			
		}
		// Iterate over all PermissionOwners
		
		log.info("Permissions deleted from all users: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllUsers()));
		log.info("Permissions deleted from all groups: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllGroups()));
		log.info("Permissions deleted from all roles: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllRoles()));
	}

	/**
	 * Deletes permission that reference non-existing executions.
	 * 
	 * @return The number of deleted permissions.
	 */
	public static int deleteQueryPermissionsWithMissingRef(MasterMetaStorage storage, Iterable<? extends PermissionOwner<?>> owners) {
		int countDeleted = 0;
		// Do the loop-di-loop
		for (PermissionOwner<?> owner : owners) {
			Set<Permission> permissions = owner.getPermissions();
			Iterator<Permission> it = permissions.iterator();
			while (it.hasNext()) {
				Permission permission = it.next();
				if (!(permission instanceof WildcardPermission)) {
					log.warn(
						"Encountered the permission type {} that is not handled by this routine. Permission was: {}",
						permission.getClass(),
						permission);
					continue;
				}
				WildcardPermission wpermission = (WildcardPermission) permission;
				if(!wpermission.getDomains().contains(QueryPermission.DOMAIN.toLowerCase())) {
					// Skip Permissions that do not reference an Execution/Query
					continue;
				}

				// Handle multiple references to instances
				Set<String> validRef = new HashSet<>();
				for (String sId : wpermission.getInstances()) {
					ManagedExecutionId mId = ManagedExecutionId.Parser.INSTANCE.parse(sId);
					if (storage.getExecution(mId) != null) {
						// Execution exists -- it is a valid reference
						validRef.add(mId.toString());
					}
				}
				if (!validRef.isEmpty()) {
					if (wpermission.getInstances().size() == validRef.size()) {
						// All are valid, nothing changed proceed with the next permission
						continue;
					}
					// Create a new Permission that only contains valid references
					WildcardPermission reducedPermission = new WildcardPermission(
						List.of(wpermission.getDomains(), wpermission.getAbilities(), validRef), wpermission.getCreationTime());
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
