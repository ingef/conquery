package com.bakdata.conquery.tasks;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.ExecutionPermission;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class PermissionCleanupTask extends Task {

    private final MetaStorage storage;

    public PermissionCleanupTask(MetaStorage storage) {
        super("permission-cleanup");
        this.storage = storage;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        log.info("Permissions deleted from all users: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllUsers()));
        log.info("Permissions deleted from all groups: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllGroups()));
        log.info("Permissions deleted from all roles: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllRoles()));
        log.info("Owned Execution permissions deleted: {}", deletePermissionsOfOwnedInstances(storage, ExecutionPermission.DOMAIN.toLowerCase(), ManagedExecutionId.Parser.INSTANCE, storage::getExecution));
        log.info("Owned FormConfig permissions deleted: {}", deletePermissionsOfOwnedInstances(storage, FormConfigPermission.DOMAIN.toLowerCase(), FormConfigId.Parser.INSTANCE, storage::getFormConfig));
    }


    /**
     * Deletes permission that reference non-existing executions.
     *
     * @return The number of deleted permissions.
     */
    public static int deleteQueryPermissionsWithMissingRef(MetaStorage storage, Iterable<? extends PermissionOwner<?>> owners) {
        int countDeleted = 0;
        // Do the loop-di-loop
        for (PermissionOwner<?> owner : owners) {
            Set<ConqueryPermission> permissions = owner.getPermissions();
			for (Permission permission : permissions) {
				WildcardPermission wpermission = getAsWildcardPermission(permission);
				if (wpermission == null) {
					continue;
				}
				if (!wpermission.getDomains().contains(ExecutionPermission.DOMAIN.toLowerCase())) {
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
					owner.addPermission(reducedPermission);
				}

				// Delete the old permission that containes both valid and invalid references
				owner.removePermission(wpermission);
				countDeleted++;

			}
        }
        return countDeleted;
    }

    @Nullable
    private static WildcardPermission getAsWildcardPermission(Permission permission) {
        if (!(permission instanceof WildcardPermission)) {
            log.warn(
                    "Encountered the permission type {} that is not handled by this routine. Permission was: {}",
                    permission.getClass(),
                    permission);
            return null;
        }
		return (WildcardPermission) permission;
    }

	/**
	 * Deletes permission that are unnecessary because the user is the owner of the referenced instance
	 *
	 * @return The number of deleted permissions.
	 */
	public static <E extends IdentifiableImpl<ID> & Owned, ID extends Id<E>> int deletePermissionsOfOwnedInstances(MetaStorage storage, String permissionDomain, IdUtil.Parser<ID> idParser, Function<ID, E> instanceStorageExtractor) {
		int countDeleted = 0;
		for (User user : storage.getAllUsers()) {
			Set<ConqueryPermission> permissions = user.getPermissions();
			for (Permission permission : permissions) {
				WildcardPermission wpermission = getAsWildcardPermission(permission);
				if (wpermission == null) {
					continue;
				}

				if (!wpermission.getDomains().contains(permissionDomain)) {
					// Skip Permissions that do not reference an Execution/Query
					continue;
				}

				if (wpermission.getInstances().size() != 1) {
					log.trace("Skipping permission {} because it refers to multiple instances.", wpermission);
				}
				ID executionId = null;
				try {
					executionId = idParser.parse(wpermission.getInstances().iterator().next());
				}
				catch (Exception e) {
					log.warn("Unable to parse an id from permission instance. Permission was: {}", wpermission);
					continue;
				}

				E execution = instanceStorageExtractor.apply(executionId);
				if (execution == null) {
					log.trace("The execution referenced in permission {} does not exist. Skipping permission");
					continue;
				}

				if (!user.isOwner(execution)) {
					log.trace("The user is not owner of the instance. Keeping the permission. User: {}, Owner: {}, Instance: {}, Permission: {}", user.getId(), execution
																																										.getOwner(), execution
																																															 .getId(), wpermission);
					continue;
				}

				log.trace("User owns the instance. Deleting the permission");
				user.removePermission(wpermission);
				countDeleted++;


			}
        }

        return countDeleted;

    }
}
