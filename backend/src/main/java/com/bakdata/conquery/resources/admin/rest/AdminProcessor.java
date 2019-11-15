package com.bakdata.conquery.resources.admin.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.Pair;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorageImpl;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.StringPermissionBuilder;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.IdMappingConfig;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.jobs.ImportJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateDataset;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.preproc.PPHeader;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Namespaces;
import com.bakdata.conquery.models.worker.SlaveInformation;
import com.bakdata.conquery.resources.admin.ui.model.FEGroupContent;
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import com.bakdata.conquery.resources.admin.ui.model.FERoleContent;
import com.bakdata.conquery.resources.admin.ui.model.FEUserContent;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.fasterxml.jackson.databind.ObjectWriter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@RequiredArgsConstructor
public class AdminProcessor {

	private final ConqueryConfig config;
	private final MasterMetaStorage storage;
	private final Namespaces namespaces;
	private final JobManager jobManager;
	private final ScheduledExecutorService maintenanceService;
	private final Validator validator;
	private final ObjectWriter jsonWriter = Jackson.MAPPER.writer();

	public void addTable(Dataset dataset, Table table) throws JSONException {
		Objects.requireNonNull(dataset);
		Objects.requireNonNull(table);
		if (table.getDataset() == null) {
			table.setDataset(dataset);
		}
		else if (!table.getDataset().equals(dataset)) {
			throw new IllegalArgumentException();
		}

		for (int p = 0; p < table.getColumns().length; p++) {
			table.getColumns()[p].setPosition(p);
		}
		table.getPrimaryColumn().setPosition(Column.PRIMARY_POSITION);
		dataset.getTables().add(table);
		namespaces.get(dataset.getId()).getStorage().updateDataset(dataset);
		namespaces.get(dataset.getId()).sendToAll(new UpdateDataset(dataset));
		// see #143 check duplicate names
	}

	public void addConcept(Dataset dataset, Concept<?> c) throws JSONException, ConfigurationException {
		c.setDataset(dataset.getId());
		if (namespaces.get(dataset.getId()).getStorage().hasConcept(c.getId())) {
			throw new WebApplicationException("Can't replace already existing concept " + c.getId(), Status.CONFLICT);
		}
		jobManager
			.addSlowJob(new SimpleJob("Adding concept " + c.getId(), () -> namespaces.get(dataset.getId()).getStorage().updateConcept(c)));
		jobManager
			.addSlowJob(new SimpleJob("sendToAll " + c.getId(), () -> namespaces.get(dataset.getId()).sendToAll(new UpdateConcept(c))));
		// see #144 check duplicate names
	}

	public Dataset addDataset(String name) throws JSONException {
		// create dataset
		Dataset dataset = new Dataset();
		dataset.setName(name);

		// add allIds table
		Table allIdsTable = new Table();
		{
			allIdsTable.setName(ConqueryConstants.ALL_IDS_TABLE);
			allIdsTable.setDataset(dataset);
			Column primaryColumn = new Column();
			{
				primaryColumn.setName(ConqueryConstants.ALL_IDS_TABLE___ID);
				primaryColumn.setPosition(0);
				primaryColumn.setTable(allIdsTable);
				primaryColumn.setType(MajorTypeId.STRING);
			}
			allIdsTable.setPrimaryColumn(primaryColumn);
		}
		dataset.getTables().add(allIdsTable);

		// store dataset in own storage
		NamespaceStorage datasetStorage = new NamespaceStorageImpl(
			storage.getValidator(),
			config.getStorage(),
			new File(storage.getDirectory().getParentFile(), "dataset_" + name));
		datasetStorage.loadData();
		datasetStorage.setMetaStorage(storage);
		Namespace ns = new Namespace(config.getCluster().getEntityBucketSize(), datasetStorage);
		ns.initMaintenance(maintenanceService);
		ns.getStorage().updateDataset(dataset);
		namespaces.add(ns);

		// for now we just add one worker to every slave
		namespaces.getSlaves().values().forEach((slave) -> {
			this.addWorker(slave, dataset);
		});
		return dataset;
	}

	public void addImport(Dataset dataset, File selectedFile) throws IOException, JSONException {
		try (HCFile hcFile = new HCFile(selectedFile, false); InputStream in = hcFile.readHeader()) {
			PPHeader header = Jackson.BINARY_MAPPER.readValue(in, PPHeader.class);

			TableId tableName = new TableId(dataset.getId(), header.getTable());
			Table table = dataset.getTables().getOrFail(tableName);

			log.info("Importing {}", selectedFile.getAbsolutePath());
			jobManager.addSlowJob(new ImportJob(namespaces.get(dataset.getId()), table.getId(), selectedFile));
		}
	}

	public void addWorker(SlaveInformation slave, Dataset dataset) {
		slave.send(new AddWorker(dataset));
	}

	public void setIdMapping(InputStream data, Namespace namespace) throws JSONException, IOException {
		try (CSV csvData = new CSV(ConqueryConfig.getInstance().getCsv().withSkipHeader(false), data)) {
			IdMappingConfig mappingConfig = config.getIdMapping();
			PersistentIdMap mapping = mappingConfig.generateIdMapping(csvData);
			namespace.getStorage().updateIdMapping(mapping);
		}
	}

	public void setStructure(Dataset dataset, StructureNode[] structure) throws JSONException {
		namespaces.get(dataset.getId()).getStorage().updateStructure(structure);
	}

	public synchronized void addRole(Role role) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(role));
		log.trace("New role:\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
		storage.addRole(role);
	}

	public void addRoles(List<Role> roles) {
		Objects.requireNonNull(roles, "Role list was empty.");
		for (Role role : roles) {
			try {
				addRole(role);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Role: %s", role), e);
			}
		}
	}

	/**
	 * Deletes the mandator, that is identified by the id. Its references are
	 * removed from the users and from the storage.
	 * 
	 * @param mandatorId
	 *            The id belonging to the mandator
	 * @throws JSONException
	 *             is thrown on JSON validation form the storage.
	 */
	public synchronized void deleteRole(RoleId mandatorId) throws JSONException {
		log.info("Deleting mandator: {}", mandatorId);
		Role mandator = storage.getRole(mandatorId);
		for (User user : storage.getAllUsers()) {
			user.removeRole(storage, mandator);
		}
		storage.removeRole(mandatorId);
	}

	public List<Role> getAllRoles() {
		return new ArrayList<>(storage.getAllRoles());
	}

	public List<User> getUsers(Role role) {
		Collection<User> user = storage.getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(role)).collect(Collectors.toList());
	}

	public FERoleContent getRoleContent(RoleId roleId) {
		Role role = Objects.requireNonNull(storage.getRole(roleId));
		return FERoleContent
			.builder()
			.permissions(wrapInFEPermission(role.copyPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.users(getUsers(role))
			.owner(roleId.getOwner(storage))
			.build();
	}

	private List<Pair<FEPermission, String>> wrapInFEPermission(Collection<ConqueryPermission> permissions) {
		List<Pair<FEPermission, String>> fePermissions = new ArrayList<>();

		for (ConqueryPermission permission : permissions) {
			if (permission instanceof WildcardPermission) {
				fePermissions.add(Pair.of(FEPermission.from((WildcardPermission) permission), permission.toString()));

			}
			else {
				log.warn("Could not create frontend representation for permission {}", permission);
			}
		}
		return fePermissions;
	}

	private Map<String, Pair<Set<Ability>, List<Object>>> preparePermissionTemplate() {
		Map<String, Pair<Set<Ability>, List<Object>>> permissionTemplateMap = new HashMap<>();

		// Grab all possible permission types for the "Create Permission" section
		Set<Class<? extends StringPermissionBuilder>> permissionTypes = CPSTypeIdResolver
			.listImplementations(StringPermissionBuilder.class);
		for (Class<? extends StringPermissionBuilder> permissionType : permissionTypes) {
			try {
				StringPermissionBuilder instance = (StringPermissionBuilder) permissionType.getField("INSTANCE").get(null);
				// Right argument is for possible targets of a specific permission type, but it
				// is left empty for now.
				permissionTemplateMap.put(instance.getDomain(), Pair.of(instance.getAllowedAbilities(), List.of()));
			}
			catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				log.error("Could not access allowed abilities for permission type: {}\n\tCause: {}", permissionType, e);
			}

		}
		return permissionTemplateMap;
	}

	/**
	 * Handles creation of permissions.
	 *
	 * @param permission
	 *            The permission to create.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	public void createPermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission) throws JSONException {
		AuthorizationHelper.addPermission(ownerId.getOwner(storage), permission, storage);
	}

	/**
	 * Handles deletion of permissions.
	 * 
	 * @param permission
	 *            The permission to delete.
	 * @throws JSONException
	 *             is thrown upon processing JSONs.
	 */
	public void deletePermission(PermissionOwnerId<?> ownerId, ConqueryPermission permission) throws JSONException {
		AuthorizationHelper.removePermission(ownerId.getOwner(storage), permission, storage);
	}

	public UIContext getUIContext() {
		return new UIContext(namespaces);
	}

	public List<User> getAllUsers() {
		return new ArrayList<>(storage.getAllUsers());
	}

	public Object getUserContent(UserId userId) {
		User user = Objects.requireNonNull(storage.getUser(userId));
		return FEUserContent
			.builder()
			.owner(user)
			.availableRoles(storage.getAllRoles())
			.permissions(wrapInFEPermission(user.copyPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.roles(user.getRoles())
			.build();
	}

	public synchronized void deleteUser(UserId userId) {
		storage.removeUser(userId);
		log.trace("Removed user {} from the storage.", userId);
	}

	public synchronized void addUser(User user) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(user));
		storage.addUser(user);
		log.trace("New user:\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
	}

	public void addUsers(List<User> users) {
		Objects.requireNonNull(users, "User list was empty.");
		for (User user : users) {
			try {
				addUser(user);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add User: %s", user), e);
			}
		}
	}

	public void deleteRoleFromUser(UserId userId, RoleId roleId) throws JSONException {
		User user = null;
		Role role = null;
		synchronized (storage) {
			user = storage.getUser(userId);
			role = storage.getRole(roleId);
		}
		Objects.requireNonNull(user);
		Objects.requireNonNull(role);
		user.removeRole(storage, role);
		log.trace("Deleted role {} to user {}", role, user);

	}

	public void addRoleToUser(UserId userId, RoleId roleId) throws JSONException {
		User user = null;
		Role role = null;
		synchronized (storage) {
			user = storage.getUser(userId);
			role = storage.getRole(roleId);
		}
		Objects.requireNonNull(user);
		Objects.requireNonNull(role);
		user.addRole(storage, role);
		log.trace("Added role {} to user {}", role, user);

	}

	public Collection<Group> getAllGroups() {
		return storage.getAllGroups();
	}

	public FEGroupContent getGroupContent(GroupId groupId) {
		Group group = Objects.requireNonNull(storage.getGroup(groupId));
		Set<User> members = group.copyMembers();
		ArrayList<User> availableMembers = new ArrayList<>(storage.getAllUsers());
		availableMembers.removeAll(members);
		return FEGroupContent
			.builder()
			.owner(group)
			.members(members)
			.availableMembers(availableMembers)
			.permissions(wrapInFEPermission(group.copyPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.build();
	}

	public synchronized void addGroup(Group group) throws JSONException {
		synchronized (storage) {
			ValidatorHelper.failOnError(log, validator.validate(group));
			storage.addGroup(group);
		}
		log.trace("New group:\tLabel: {}\tName: {}\tId: {} ", group.getLabel(), group.getName(), group.getId());

	}

	public void addGroups(List<Group> groups) {
		Objects.requireNonNull(groups, "Group list was null.");
		for (Group group : groups) {
			try {
				addGroup(group);
			}
			catch (Exception e) {
				log.error(String.format("Failed to add Group: %s", group), e);
			}
		}
	}

	public void addUserToGroup(GroupId groupId, UserId userId) throws JSONException {
		synchronized (storage) {
			Objects.requireNonNull(groupId.getOwner(storage)).addMember(storage, Objects.requireNonNull(userId.getOwner(storage)));
		}
		log.trace("Added user {} to group {}", userId.getOwner(storage), groupId.getOwner(getStorage()));
	}

	public void deleteUserFromGroup(GroupId groupId, UserId userId) throws JSONException {
		synchronized (storage) {
			Objects.requireNonNull(groupId.getOwner(storage)).removeMember(storage, Objects.requireNonNull(userId.getOwner(storage)));
		}
		log.trace("Removed user {} from group {}", userId.getOwner(storage), groupId.getOwner(getStorage()));
	}

	public void removeGroup(GroupId groupId) {
		synchronized (storage) {
			storage.removeGroup(groupId);
		}
		log.trace("Removed group {}", groupId.getOwner(getStorage()));
	}
}
