package com.bakdata.conquery.resources.admin.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AdminUIProcessor {

	private final MasterMetaStorage storage;

	public void createMandator(String name, String idString) throws JSONException {

		log.info("New mandator:\tName: {}\tId: {} ", name, idString);
		Mandator mandator = new Mandator(idString, name);
		storage.addMandator(mandator);
	}

	public List<Mandator> getAllMandators() {
		return new ArrayList<>(storage.getAllMandators());
	}

	public List<User> getUsers(MandatorId mandatorId) {
		Mandator mandator = (Mandator) mandatorId.getOwner(storage);
		Collection<User> user = storage.getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(mandator)).collect(Collectors.toList());
	}

	public List<ConqueryPermission> getPermissions(PermissionOwnerId<?> id) {
		PermissionOwner<?> owner = id.getOwner(storage);
		return new ArrayList<>(owner.getPermissions());
	}

	public FEMandatorContent getMandatorContent(MandatorId mandatorId) {
		List<ConqueryPermission> permissions = getPermissions(mandatorId);
		List<DatasetPermission> datasetPermissions = new ArrayList<>();
		List<QueryPermission> queryPermissions = new ArrayList<>();
		List<ConqueryPermission> otherPermissions = new ArrayList<>();

		for (ConqueryPermission permission : permissions) {
			if (permission instanceof DatasetPermission) {
				datasetPermissions.add((DatasetPermission) permission);
			}
			else if (permission instanceof QueryPermission) {
				queryPermissions.add((QueryPermission) permission);
			}
			else {
				otherPermissions.add(permission);
			}
		}
		
		List<Dataset> datasets = storage.getNamespaces().getAllDatasets();

		return new FEMandatorContent(
			(Mandator)mandatorId.getOwner(storage),
			getUsers(mandatorId),
			datasetPermissions,
			queryPermissions,
			otherPermissions,
			Ability.READ.asSet(),
			datasets);
	}
	
	/**
	 * Handles creation of permissions.
	 * @param permission The permission to create.
	 * @throws JSONException is thrown upon procession JSONs.
	 */
	public void createPermission(ConqueryPermission permission) throws JSONException {
		AuthorizationHelper.addPermission(getOwnerFromPermission(permission, storage), permission, storage);
	}
	
	/**
	 * Handles deletion of permissions.
	 * @param permission The permission to delete.
	 * @throws JSONException is thrown upon procession JSONs.
	 */
	public void deletePermission(ConqueryPermission permission) throws JSONException {
		AuthorizationHelper.removePermission(getOwnerFromPermission(permission, storage), permission, storage);
	}
	
	/**
	 * Retrieves the {@link PermissionOwner} from an permission that should be created or deleted.
	 * @param permission The permission with an owner.
	 * @param storage A storage from which the owner is retrieved.
	 * @return The Owner.
	 */
	private static PermissionOwner<?> getOwnerFromPermission(ConqueryPermission permission, MasterMetaStorage storage) {
		PermissionOwnerId<?> ownerId = permission.getOwnerId();
		if(ownerId == null) {
			throw new IllegalArgumentException("The ownerId is not allowed to be null.");
		}
		PermissionOwner<?> owner =  ownerId.getOwner(storage);
		if(owner == null) {
			throw new IllegalArgumentException("The provided ownerId belongs to know subject.");
		}
		return owner;
	}
}
