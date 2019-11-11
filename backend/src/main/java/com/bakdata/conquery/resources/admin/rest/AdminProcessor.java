package com.bakdata.conquery.resources.admin.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
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
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.csv.CSV;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorageImpl;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.HasTarget;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.auth.permissions.SuperPermission;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.ConfigurationException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
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
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import com.bakdata.conquery.resources.admin.ui.model.FERoleContent;
import com.bakdata.conquery.resources.admin.ui.model.FERoleContent.FERoleContentBuilder;
import com.bakdata.conquery.resources.admin.ui.model.FEUserContent;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.fasterxml.jackson.core.JsonProcessingException;
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
		log.info("New mandator:\\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
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

	public List<User> getUsers(RoleId mandatorId) {
		Role mandator = (Role) mandatorId.getOwner(storage);
		Collection<User> user = storage.getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(mandator)).collect(Collectors.toList());
	}

	public Set<ConqueryPermission> getPermissions(PermissionOwnerId<?> id) {
		PermissionOwner<?> owner = id.getOwner(storage);
		return Objects.requireNonNull(owner,"PermissionOwner not found.").copyPermissions();
	}

	public FERoleContent getRoleContent(RoleId mandatorId) throws JsonProcessingException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return FERoleContent.builder()
			.permissions(wrapInFEPermission(getPermissions(mandatorId)))
			.permissionTemplateMap(preparePermissionTemplate())
			.users(getUsers(mandatorId))
			.owner(mandatorId.getOwner(storage))
			.build();
	}

	private List<Pair<FEPermission, String>> wrapInFEPermission(Collection<ConqueryPermission> permissions) throws JsonProcessingException {
		List<Pair<FEPermission,String>> fePermissions = new ArrayList<>();

		for (ConqueryPermission permission : permissions) {
			fePermissions.add(Pair.of(FEPermission.from(permission), jsonWriter.writeValueAsString(permission)));
		}
		return fePermissions;
	}

	private Map<String, Pair<Set<Ability>, List<Object>>> preparePermissionTemplate() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Map<String, Pair<Set<Ability>,List<Object>>> permissionTemplateMap = new HashMap<>();

		// Grab all possible permission types for the "Create Permission" section 
		Set<Class<? extends ConqueryPermission>> permissionTypes = CPSTypeIdResolver.listImplementations(ConqueryPermission.class);
		for (Class<? extends ConqueryPermission> permissionType : permissionTypes) {
			// Get the possible targets and abilities for a permission
			List<Object> targetObjects = new ArrayList<>();
			Set<Ability> abilities = EnumSet.noneOf(Ability.class);
			if (HasTarget.class.isAssignableFrom(permissionType)) {
				targetObjects.addAll(getTargets(permissionType));
			}
			if (DatasetPermission.class.isAssignableFrom(permissionType)) {
				//targetObjects.addAll(storage.getNamespaces().getAllDatasets().stream().map(Identifiable::getId).collect(Collectors.toList()));
				//targetObjects.addAll(getTargets(permissionType));
				abilities.addAll(DatasetPermission.ALLOWED_ABILITIES);
			}
			else if (QueryPermission.class.isAssignableFrom(permissionType)) {
				//targetObjects.addAll(storage.getAllExecutions().stream().map(Identifiable::getId).collect(Collectors.toList()));
				abilities.addAll(QueryPermission.ALLOWED_ABILITIES);
			}
			CPSType anno = permissionType.getAnnotation(CPSType.class);
			permissionTemplateMap.put(anno.id(), Pair.of(abilities, targetObjects));
			
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

	public Object getUserContent(UserId userId) throws JsonProcessingException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		User user = storage.getUser(userId);
		return FEUserContent.builder()
			.owner(user)
			.availableRoles(storage.getAllRoles()	)
			.permissions(wrapInFEPermission(getPermissions(userId)))
			.permissionTemplateMap(preparePermissionTemplate())
			.roles(user.getRoles())
			.build();
	}

	public synchronized void deleteUser(UserId userId) {
		storage.removeUser(userId);
	}

	public synchronized void addUser(User user) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(user));
		log.info("New mandator:\\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
		storage.addUser(user);
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

	}
	

	List getTargets(Class<? extends ConqueryPermission> permissionClass) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = permissionClass.getMethod("getPossibleTargets", MasterMetaStorage.class);
		
		return List.class.cast(method.invoke(null, storage));
	}
}
