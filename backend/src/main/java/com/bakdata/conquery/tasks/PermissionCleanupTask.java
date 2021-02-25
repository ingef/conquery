package com.bakdata.conquery.tasks;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.FormConfigPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.execution.Owned;
import com.bakdata.conquery.models.identifiable.IdentifiableImpl;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.FormConfigId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import io.dropwizard.servlets.tasks.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.Permission;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;

@Slf4j
public class PermissionCleanupTask extends Task {

    private final MetaStorage storage;

    protected PermissionCleanupTask(MetaStorage storage) {
        super("permission-cleanup");
        this.storage = storage;
    }

    @Override
    public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
        log.info("Permissions deleted from all users: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllUsers()));
        log.info("Permissions deleted from all groups: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllGroups()));
        log.info("Permissions deleted from all roles: {}", deleteQueryPermissionsWithMissingRef(storage, storage.getAllRoles()));
        log.info("Owned Execution permissions deleted: {}", deletePermissionsOfOwnedInstances(storage, QueryPermission.DOMAIN.toLowerCase(), ManagedExecutionId.Parser.INSTANCE, storage::getExecution));
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
            Set<Permission> permissions = owner.getPermissions();
            Iterator<Permission> it = permissions.iterator();
            while (it.hasNext()) {
                Permission permission = it.next();
                WildcardPermission wpermission = getAsWildcardPermission(permission);
                if (wpermission == null) {
                    continue;
                }
                if (!wpermission.getDomains().contains(QueryPermission.DOMAIN.toLowerCase())) {
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

    @Nullable
    private static WildcardPermission getAsWildcardPermission(Permission permission) {
        if (!(permission instanceof WildcardPermission)) {
            log.warn(
                    "Encountered the permission type {} that is not handled by this routine. Permission was: {}",
                    permission.getClass(),
                    permission);
            return null;
        }
        WildcardPermission wpermission = (WildcardPermission) permission;
        return wpermission;
    }

    /**
     * Deletes permission that are unnecessary because the user is the owner of the referenced instance
     *
     * @return The number of deleted permissions.
     */
    public static <E extends IdentifiableImpl<?> & Owned, ID extends IId<E>> int deletePermissionsOfOwnedInstances(MetaStorage storage, String permissionDomain, IId.Parser<ID> idParser, Function<ID, E> instanceStorageExtractor) {
        int countDeleted = 0;
        for (User user : storage.getAllUsers()) {
            Set<Permission> permissions = user.getPermissions();
            Iterator<Permission> it = permissions.iterator();
            while (it.hasNext()) {
                Permission permission = it.next();
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
                } catch (Exception e) {
                    log.warn("Unable to parse an id from permission instance. Permission was: {}", wpermission);
                    continue;
                }

                E execution = instanceStorageExtractor.apply(executionId);
                if (execution == null) {
                    log.trace("The execution referenced in permission {} does not exist. Skipping permission");
                    continue;
                }

                if (!user.isOwner(execution)) {
                    log.trace("The user is not owner of the instance. Keeping the permission. User: {}, Owner: {}, Instance: {}, Permission: {}", user.getId(), execution.getOwner(), execution.getId(), wpermission);
                    continue;
                }

                log.trace("User owns the instance. Deleting the permission");
                user.removePermission(storage, wpermission);
                countDeleted++;


            }
        }

        return countDeleted;

    }
}
