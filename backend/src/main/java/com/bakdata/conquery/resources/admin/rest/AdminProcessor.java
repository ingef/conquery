package com.bakdata.conquery.resources.admin.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.validation.Validator;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.apiv1.FilterSearch;
import com.bakdata.conquery.io.HCFile;
import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.csv.CsvIo;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorage;
import com.bakdata.conquery.io.xodus.NamespaceStorageImpl;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.RoleOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.StringPermissionBuilder;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.StructureNode;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.SecondaryIdDescription;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.specific.ConceptId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdDescriptionId;
import com.bakdata.conquery.models.identifiable.ids.specific.TableId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.identifiable.mapping.PersistentIdMap;
import com.bakdata.conquery.models.jobs.ImportJob;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.SimpleJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveTable;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateConcept;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateMatchingStatsMessage;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateSecondaryId;
import com.bakdata.conquery.models.messages.namespaces.specific.UpdateTable;
import com.bakdata.conquery.models.messages.network.specific.AddWorker;
import com.bakdata.conquery.models.messages.network.specific.RemoveWorker;
import com.bakdata.conquery.models.preproc.PreprocessedHeader;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.resources.admin.ui.model.FEAuthOverview;
import com.bakdata.conquery.resources.admin.ui.model.FEAuthOverview.OverviewRow;
import com.bakdata.conquery.resources.admin.ui.model.FEGroupContent;
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import com.bakdata.conquery.resources.admin.ui.model.FERoleContent;
import com.bakdata.conquery.resources.admin.ui.model.FEUserContent;
import com.bakdata.conquery.resources.admin.ui.model.UIContext;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.collect.Multimap;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvWriter;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.Permission;

/**
 * This class holds the logic for several admin http endpoints.
 */
// TODO clean up methods and refactor
@Getter
@Slf4j
@RequiredArgsConstructor
public class AdminProcessor {

	private final ConqueryConfig config;
	private final MetaStorage storage;
	private final DatasetRegistry datasetRegistry;
	private final JobManager jobManager;
	private final ScheduledExecutorService maintenanceService;
	private final Validator validator;
	private final ObjectWriter jsonWriter = Jackson.MAPPER.writer();
	private final int entityBucketSize;

	public synchronized void addTable(Table table, Namespace namespace) throws JSONException {
		Dataset dataset = namespace.getDataset();

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


		namespace.getStorage().addTable(table);
		namespace.sendToAll(new UpdateTable(table));
	}

	public synchronized void addConcept(@NonNull Dataset dataset, @NonNull Concept<?> concept) throws JSONException {
		concept.setDataset(dataset.getId());
		ValidatorHelper.failOnError(log, validator.validate(concept));
		// Register the Concept in the ManagerNode and Workers
		if (datasetRegistry.get(dataset.getId()).getStorage().hasConcept(concept.getId())) {
			throw new WebApplicationException("Can't replace already existing concept " + concept.getId(), Status.CONFLICT);
		}

		datasetRegistry.get(dataset.getId()).getStorage().updateConcept(concept);
		datasetRegistry.get(dataset.getId()).sendToAll(new UpdateConcept(concept));
	}

	public synchronized Dataset addDataset(String name) throws JSONException {
		// create dataset
		Dataset dataset = new Dataset();
		dataset.setName(name);

		// store dataset in own storage
		NamespaceStorage datasetStorage = new NamespaceStorageImpl(
			storage.getValidator(),
			config.getStorage(),
			new File(config.getStorage().getDirectory(), "dataset_" + name));
		datasetStorage.loadData();
		datasetStorage.setMetaStorage(storage);
		datasetStorage.updateDataset(dataset);

		Namespace ns = new Namespace(datasetStorage);
		ns.initMaintenance(maintenanceService);

		datasetRegistry.add(ns);

		// for now we just add one worker to every ShardNode
		for (ShardNodeInformation node : datasetRegistry.getShardNodes().values()) {
			addWorker(node, dataset);
		}

		return dataset;
	}

	public void addImport(Namespace namespace, File selectedFile) throws IOException {
		Dataset ds = namespace.getDataset();

		try (HCFile hcFile = new HCFile(selectedFile, false); InputStream in = hcFile.readHeader()) {
			PreprocessedHeader header = Jackson.BINARY_MAPPER.readValue(in, PreprocessedHeader.class);

			TableId tableName = new TableId(ds.getId(), header.getTable());
			Table table = namespace.getStorage().getTable(tableName);

			final ImportId importId = new ImportId(table.getId(), header.getName());

			if(datasetRegistry.get(ds.getId()).getStorage().getImport(importId) != null){
				throw new IllegalArgumentException(String.format("Import[%s] is already present.", importId));
			}

			log.info("Importing {}", selectedFile.getAbsolutePath());

			datasetRegistry.get(ds.getId()).getJobManager()
					  .addSlowJob(new ImportJob(datasetRegistry.get(ds.getId()), table, selectedFile, entityBucketSize));
		}
	}

	public void addWorker(ShardNodeInformation node, Dataset dataset) {
		node.send(new AddWorker(dataset));
	}

	public void setIdMapping(InputStream data, Namespace namespace) throws JSONException, IOException {
		CsvParser parser = new CsvParser(ConqueryConfig.getInstance().getCsv()
													   .withSkipHeader(false)
													   .withParseHeaders(false)
													   .createCsvParserSettings());

		PersistentIdMap mapping = config.getIdMapping().generateIdMapping(parser.iterate(data).iterator());

		namespace.getStorage().updateIdMapping(mapping);
	}

	public void setStructure(Dataset dataset, StructureNode[] structure) throws JSONException {
		datasetRegistry.get(dataset.getId()).getStorage().updateStructure(structure);
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
	 * removed from the users, the groups, and from the storage.
	 *
	 * @param roleId
	 *            The id belonging to the mandator
	 * @throws JSONException
	 *             is thrown on JSON validation form the storage.
	 */
	public void deleteRole(RoleId roleId) throws JSONException {
		AuthorizationHelper.deleteRole(storage, roleId);
	}

	public SortedSet<Role> getAllRoles() {
		return new TreeSet<>(storage.getAllRoles());
	}

	public List<User> getUsers(Role role) {
		Collection<User> user = storage.getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(role)).collect(Collectors.toList());
	}

	private List<Group> getGroups(Role role) {
		Collection<Group> groups = storage.getAllGroups();
		return groups.stream().filter(g -> g.getRoles().contains(role)).collect(Collectors.toList());
	}

	public FERoleContent getRoleContent(RoleId roleId) {
		Role role = Objects.requireNonNull(roleId.getPermissionOwner(storage));
		return FERoleContent
			.builder()
			.permissions(wrapInFEPermission(role.getPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.users(getUsers(role))
			.groups(getGroups(role))
			.owner(role)
			.build();
	}

	private SortedSet<FEPermission> wrapInFEPermission(Collection<Permission> permissions) {
		TreeSet<FEPermission> fePermissions = new TreeSet<>();

		for (Permission permission : permissions) {
			if (permission instanceof ConqueryPermission) {
				fePermissions.add(FEPermission.from((ConqueryPermission)permission));

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
		AuthorizationHelper.addPermission(ownerId.getPermissionOwner(storage), permission, storage);
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
		AuthorizationHelper.removePermission(ownerId.getPermissionOwner(storage), permission, storage);
	}

	public UIContext getUIContext() {
		return new UIContext(datasetRegistry);
	}

	public TreeSet<User> getAllUsers() {
		return new TreeSet<>(storage.getAllUsers());
	}

	public FEUserContent getUserContent(UserId userId) {
		User user = Objects.requireNonNull(storage.getUser(userId));
		return FEUserContent
			.builder()
			.owner(user)
			.roles(user.getRoles().stream().map(storage::getRole).collect(Collectors.toList()))
			.availableRoles(storage.getAllRoles())
			.permissions(wrapInFEPermission(user.getPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.build();
	}

	public synchronized void deleteUser(UserId userId) {
		User user = storage.getUser(userId);
		for (Group group : storage.getAllGroups()) {
			group.removeMember(storage, user);
		}
		storage.removeUser(userId);
		log.trace("Removed user {} from the storage.", userId);
	}

	public void addUser(User user) throws JSONException {
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

	public TreeSet<Group> getAllGroups() {
		return new TreeSet<>(storage.getAllGroups());
	}

	public FEGroupContent getGroupContent(GroupId groupId) {
		Group group = Objects.requireNonNull(storage.getGroup(groupId));
		Set<UserId> membersIds = group.getMembers();
		ArrayList<User> availableMembers = new ArrayList<>(storage.getAllUsers());
		availableMembers.removeIf(u -> membersIds.contains(u.getId()));
		return FEGroupContent
			.builder()
			.owner(group)
			.members(membersIds.stream().map(storage::getUser).collect(Collectors.toList()))
			.availableMembers(availableMembers)
			.roles(group.getRoles().stream().map(storage::getRole).collect(Collectors.toList()))
			.availableRoles(storage.getAllRoles())
			.permissions(wrapInFEPermission(group.getPermissions()))
			.permissionTemplateMap(preparePermissionTemplate())
			.build();
	}

	public synchronized void addGroup(Group group) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(group));
		storage.addGroup(group);
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

	public void addUserToGroup(GroupId groupId, UserId userId) {
		synchronized (storage) {
			Objects
				.requireNonNull(groupId.getPermissionOwner(storage))
				.addMember(storage, Objects.requireNonNull(userId.getPermissionOwner(storage)));
		}
		log.trace("Added user {} to group {}", userId.getPermissionOwner(storage), groupId.getPermissionOwner(storage));
	}

	public void deleteUserFromGroup(GroupId groupId, UserId userId) {
		synchronized (storage) {
			Objects
				.requireNonNull(groupId.getPermissionOwner(storage))
				.removeMember(storage, Objects.requireNonNull(userId.getPermissionOwner(storage)));
		}
		log.trace("Removed user {} from group {}", userId.getPermissionOwner(storage), groupId.getPermissionOwner(storage));
	}

	public void deleteGroup(GroupId groupId) {
		storage.removeGroup(groupId);
		log.trace("Removed group {}", groupId.getPermissionOwner(storage));
	}

	public void deleteRoleFrom(PermissionOwnerId<?> ownerId, RoleId roleId) {
		PermissionOwner<?> owner = null;
		Role role = null;
		synchronized (storage) {
			owner = Objects.requireNonNull(ownerId.getPermissionOwner(storage));
			role = Objects.requireNonNull(storage.getRole(roleId));
		}
		if (!(owner instanceof RoleOwner)) {
			throw new IllegalStateException(String.format("Provided entity %s cannot hold any roles", owner));
		}
		((RoleOwner) owner).removeRole(storage, role);
		log.trace("Deleted role {} from {}", role, owner);
	}

	public void addRoleTo(PermissionOwnerId<?> ownerId, RoleId roleId) {
		AuthorizationHelper.addRoleTo(getStorage(), ownerId, roleId);
	}

	public FEAuthOverview getAuthOverview() {
		Collection<OverviewRow> overview = new TreeSet<>();
		for (User user : storage.getAllUsers()) {
			Collection<Group> userGroups = AuthorizationHelper.getGroupsOf(user, storage);
			List<Role> effectiveRoles = user.getRoles().stream().map(storage::getRole).collect(Collectors.toList());
			userGroups.forEach(g -> {
				effectiveRoles.addAll(g.getRoles().stream().map(storage::getRole).collect(Collectors.toList()));
			});
			overview.add(OverviewRow.builder().user(user).groups(userGroups).effectiveRoles(effectiveRoles).build());
		}

		return FEAuthOverview.builder().overview(overview).build();
	}

	/**
	 * Renders the permission overview for all users in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV() {
		return getPermissionOverviewAsCSV(storage.getAllUsers());
	}


	/**
	 * Renders the permission overview for all users in a certain {@link Group} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(GroupId groupId) {
		Group group = Objects.requireNonNull(storage.getGroup(groupId), "The group was not found");
		return getPermissionOverviewAsCSV(group.getMembers().stream().map(storage::getUser).collect(Collectors.toList()));
	}

	/**
	 * Renders the permission overview for certian {@link User} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(Collection<User> users) {
		StringWriter sWriter = new StringWriter();
		CsvWriter writer = CsvIo.createWriter(sWriter);
		List<String> scope = ConqueryConfig.getInstance()
			.getAuthorization()
			.getOverviewScope();
		// Header
		writeAuthOverviewHeader(writer, scope);
		// Body
		for (User user : users) {
			writeAuthOverviewUser(writer, scope, user, storage);
		}
		return sWriter.toString();
	}

	/**
	 * Writes the header of the CSV auth overview to the specified writer.
	 */
	private static void writeAuthOverviewHeader(CsvWriter writer, List<String> scope) {
		List<String> headers = new ArrayList<>();
		headers.add("User");
		headers.addAll(scope);
		writer.writeHeaders(headers);
	}

	/**
	 * Writes the given {@link User}s (one perline) with their effective permission to the specified CSV writer.
	 */
	private static void writeAuthOverviewUser(CsvWriter writer, List<String> scope, User user, MetaStorage storage) {
		// Print the user in the first column
		writer.addValue(String.format("%s %s", user.getLabel(), ConqueryEscape.unescape(user.getName())));

		// Print the permission per domain in the remaining columns
		Multimap<String, ConqueryPermission> permissions = AuthorizationHelper.getEffectiveUserPermissions(user.getId(), scope , storage);
		for(String domain : scope) {
			writer.addValue(permissions.get(domain).stream()
				.map(Object::toString)
				.collect(Collectors.joining(ConqueryConfig.getInstance().getCsv().getLineSeparator())));
		}
		writer.writeValuesToRow();
	}

	public synchronized void deleteImport(ImportId importId) {
		// TODO explain when the includedBucket Information is updated/cleared in the WorkerInformation

		final Namespace namespace = datasetRegistry.get(importId.getDataset());


		namespace.getStorage().removeImport(importId);
		namespace.sendToAll(new RemoveImportJob(importId));

		// Remove bucket assignments for consistency report
		namespace.removeBucketAssignmentsForImportFormWorkers(importId);
	}

	public synchronized void deleteTable(TableId tableId)  {
		final Namespace namespace = datasetRegistry.get(tableId.getDataset());

		final List<? extends Connector> connectors = namespace.getStorage().getAllConcepts().stream().flatMap(c -> c.getConnectors().stream())
															  .filter(con -> con.getTable().getId().equals(tableId))
															  .collect(Collectors.toList());

		if(!connectors.isEmpty()) {
			throw new IllegalArgumentException(String.format("Cannot delete table `%s`, because it still has connectors for Concepts: `%s`", tableId, connectors.stream().map(Connector::getConcept).collect(Collectors.toList())));
		}


		namespace.getStorage().getAllImports().stream()
				 .filter(imp -> imp.getTable().equals(tableId))
				 .map(Import::getId)
				 .forEach(this::deleteImport);

		namespace.getStorage().removeTable(tableId);
		namespace.sendToAll(new RemoveTable(tableId));
	}

	public synchronized void deleteConcept(ConceptId conceptId) {
		final Namespace namespace = datasetRegistry.get(conceptId.getDataset());

		namespace.getStorage().removeConcept(conceptId);
		getJobManager()
				.addSlowJob(new SimpleJob("sendToAll: remove " + conceptId, () -> namespace.sendToAll(new RemoveConcept(conceptId))));
	}

	public synchronized void deleteDataset(DatasetId datasetId) {
		final Namespace namespace = datasetRegistry.get(datasetId);

		if(!namespace.getStorage().getTables().isEmpty()){
			throw new IllegalArgumentException(
					String.format("Cannot delete dataset `%s`, because it still has tables: `%s`",
								  datasetId,
								  namespace.getStorage().getTables().stream()
										   .map(Table::getId)
										   .map(Objects::toString)
										   .collect(Collectors.joining(","))
					));
		}


		datasetRegistry.removeNamespace(datasetId);
		datasetRegistry.getShardNodes().values().forEach( shardNode -> shardNode.send(new RemoveWorker(datasetId)));

	}

	public void updateMatchingStats(DatasetId datasetId) {
		final Namespace ns = getDatasetRegistry().get(datasetId);

		ns.sendToAll(new UpdateMatchingStatsMessage());
		FilterSearch.updateSearch(getDatasetRegistry(), Collections.singleton(ns.getDataset()), getJobManager());
	}

	public synchronized void addSecondaryId(Namespace namespace, SecondaryIdDescription secondaryId) {
		final Dataset dataset = namespace.getDataset();
		secondaryId.setDataset(dataset);

		log.info("Received new SecondaryId[{}]", secondaryId.getId());

		namespace.getStorage().addSecondaryId(secondaryId);

		namespace.sendToAll(new UpdateSecondaryId(secondaryId));
	}

	public synchronized void deleteSecondaryId(SecondaryIdDescriptionId secondaryId) {
		final Namespace namespace = datasetRegistry.get(secondaryId.getDataset());

		// Before we commit this deletion, we check if this SecondaryId still has dependent Columns.
		final List<Column> dependents = namespace.getStorage().getTables().stream()
												 .map(Table::getColumns).flatMap(Arrays::stream)
												 .filter(column -> column.getSecondaryId() != null)
												 .filter(column -> column.getSecondaryId().getId().equals(secondaryId))
												 .collect(Collectors.toList());

		if(!dependents.isEmpty()){
			log.error(
					"SecondaryId[{}] still present on {}",
					secondaryId,
					dependents.stream().map(Column::getTable).map(Identifiable::getId).collect(Collectors.toSet())
			);

			throw new ForbiddenException("SecondaryId still has dependencies.");
		}

		log.info("Deleting SecondaryId[{}]", secondaryId);

		namespace.getStorage().removeSecondaryId(secondaryId);
		namespace.sendToAll(new RemoveSecondaryId(secondaryId));
	}
}
