package com.bakdata.conquery.resources.admin.rest;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.Validator;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.PermissionOwnerId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.index.IndexKey;
import com.bakdata.conquery.models.jobs.JobManager;
import com.bakdata.conquery.models.jobs.JobManagerStatus;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.ShardNodeInformation;
import com.bakdata.conquery.util.ConqueryEscape;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.cache.CacheStats;
import com.google.common.collect.Multimap;
import com.univocity.parsers.csv.CsvWriter;
import groovy.lang.GroovyShell;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

/**
 * This class holds the logic for several admin http endpoints.
 */
// TODO clean up methods and refactor
@Getter
@Slf4j
@RequiredArgsConstructor
public class AdminProcessor {

	private final ManagerNode managerNode;
	private final ConqueryConfig config;
	private final MetaStorage storage;
	private final DatasetRegistry<? extends Namespace> datasetRegistry;
	private final JobManager jobManager;
	private final ScheduledExecutorService maintenanceService;
	private final Validator validator;
	private final ObjectWriter jsonWriter = Jackson.MAPPER.writer();
	private final Supplier<Collection<ShardNodeInformation>> nodeProvider;

	public synchronized void addRole(Role role) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(role));
		log.trace("New role:\tLabel: {}\tName: {}\tId: {} ", role.getLabel(), role.getName(), role.getId());
		storage.addRole(role);
	}

	/**
	 * Deletes the mandator, that is identified by the id. Its references are
	 * removed from the users, the groups, and from the storage.
	 *
	 * @param role the role to delete
	 */
	public void deleteRole(RoleId role) {
		log.info("Deleting {}", role);

		try (Stream<User> allUsers = storage.getAllUsers(); Stream<Group> allGroups = storage.getAllGroups()) {
			allUsers.forEach(user -> user.removeRole(role));
			allGroups.forEach(group -> group.removeRole(role));
		}
		storage.removeRole(role);
	}

	public SortedSet<Role> getAllRoles() {
		return storage.getAllRoles().collect(Collectors.toCollection(TreeSet::new));
	}


	/**
	 * Handles creation of permissions.
	 *
	 * @param owner      to which the permission is assigned
	 * @param permission The permission to create.
	 * @throws JSONException is thrown upon processing JSONs.
	 */
	public void createPermission(PermissionOwnerId<?> owner, ConqueryPermission permission) throws JSONException {
		owner.resolve().addPermission(permission);
	}

	/**
	 * Handles deletion of permissions.
	 *
	 * @param owner      the owner of the permission
	 * @param permission The permission to delete.
	 */
	public void deletePermission(PermissionOwnerId<?> owner, ConqueryPermission permission) {
		owner.resolve().removePermission(permission);
	}


	public TreeSet<User> getAllUsers() {
		return storage.getAllUsers().collect(Collectors.toCollection(TreeSet::new));
	}

	public synchronized void deleteUser(UserId user) {
		try (Stream<Group> allGroups = storage.getAllGroups()) {
			allGroups.forEach(group -> group.removeMember(user));
		}
		storage.removeUser(user);
		log.trace("Removed user {} from the storage.", user);
	}

	public void addUsers(List<User> users) {

		for (User user : users) {
			try {
				addUser(user);
			}
			catch (Exception e) {
				log.error("Failed to add User: {}", user, e);
			}
		}
	}

	public void addUser(User user) {
		storage.addUser(user);
		log.trace("New user:\tLabel: {}\tName: {}\tId: {} ", user.getLabel(), user.getName(), user.getId());
	}

	public TreeSet<Group> getAllGroups() {
		return storage.getAllGroups().collect(Collectors.toCollection(TreeSet::new));
	}

	public void addGroups(List<Group> groups) {
		for (Group group : groups) {
			try {
				addGroup(group);
			}
			catch (Exception e) {
				log.error("Failed to add Group: {}", group, e);
			}
		}
	}

	public synchronized void addGroup(Group group) throws JSONException {
		ValidatorHelper.failOnError(log, validator.validate(group));
		storage.addGroup(group);
		log.trace("New group:\tLabel: {}\tName: {}\tId: {} ", group.getLabel(), group.getName(), group.getId());

	}

	public void addUserToGroup(GroupId groupId, UserId user) {
		final Group group = groupId.resolve();

		group.addMember(user);
		log.trace("Added user {} to group {}", user, group);
	}

	public void deleteUserFromGroup(GroupId groupId, UserId user) {
		final Group group = groupId.resolve();

		group.removeMember(user);
		log.trace("Removed user {} from group {}", user, group);
	}

	public void deleteGroup(GroupId group) {
		storage.removeGroup(group);
		log.trace("Removed group {}", group);
	}

	public void deleteRoleFromGroup(GroupId owner, RoleId role) {
		owner.resolve().removeRole(role);
		log.trace("Removed role {} from {}", role, owner);
	}

	public void addRoleToGroup(GroupId owner, RoleId role) {
		owner.resolve().addRole(role);
		log.trace("Added role {} to {}", role, owner);
	}

	public void addRoleToUser(UserId owner, RoleId role) {
		owner.resolve().addRole(role);
		log.trace("Added role {} to {}", role, owner);
	}

	public void deleteRoleFromUser(UserId owner, RoleId role) {
		owner.resolve().removeRole(role);
		log.trace("Removed role {} from {}", role, owner);
	}

	/**
	 * Renders the permission overview for all users in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV() {
		return getPermissionOverviewAsCSV(storage.getAllUsers());
	}

	/**
	 * Renders the permission overview for certain {@link User} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(Stream<User> users) {
		final StringWriter sWriter = new StringWriter();
		final CsvWriter writer = config.getCsv().createWriter(sWriter);
		final List<String> scope = config
				.getAuthorizationRealms()
				.getOverviewScope();
		// Header
		writeAuthOverviewHeader(writer, scope);
		// Body
		users.forEach(user ->
							  writeAuthOverviewUser(writer, scope, user, storage, config)
		);
		return sWriter.toString();
	}

	/**
	 * Writes the header of the CSV auth overview to the specified writer.
	 */
	private static void writeAuthOverviewHeader(CsvWriter writer, List<String> scope) {
		final List<String> headers = new ArrayList<>();
		headers.add("User");
		headers.addAll(scope);
		writer.writeHeaders(headers);
	}

	/**
	 * Writes the given {@link User}s (one per line) with their effective permission to the specified CSV writer.
	 */
	private static void writeAuthOverviewUser(CsvWriter writer, List<String> scope, User user, MetaStorage storage, ConqueryConfig config) {
		// Print the user in the first column
		writer.addValue(String.format("%s %s", user.getLabel(), ConqueryEscape.unescape(user.getName())));

		// Print the permission per domain in the remaining columns
		final Multimap<String, ConqueryPermission> permissions = AuthorizationHelper.getEffectiveUserPermissions(user, scope, storage);
		for (String domain : scope) {
			writer.addValue(permissions.get(domain).stream()
									   .map(Object::toString)
									   .collect(Collectors.joining(config.getCsv().getLineSeparator())));
		}
		writer.writeValuesToRow();
	}

	/**
	 * Renders the permission overview for all users in a certain {@link Group} in form of a CSV.
	 */
	public String getPermissionOverviewAsCSV(GroupId groupId) {
		final Group group = storage.getGroup(groupId);
		return getPermissionOverviewAsCSV(group.getMembers().stream().map(Id::get));
	}

	public boolean isBusy() {
		//Note that this does not and cannot check for fast jobs!
		return getJobs().stream()
						.map(JobManagerStatus::getJobs)
						.anyMatch(Predicate.not(Collection::isEmpty));
	}

	public Collection<JobManagerStatus> getJobs() {
		final List<JobManagerStatus> out = new ArrayList<>();


		out.add(JobManagerStatus.builder()
								.origin("Manager")
								.jobs(getJobManager().getJobStatus())
								.build());

		for (Namespace namespace : getDatasetRegistry().getNamespaces()) {
			out.add(JobManagerStatus.builder()
									.origin("Manager")
									.dataset(namespace.getDataset().getId())
									.jobs(namespace.getJobManager().getJobStatus())
									.build());
		}

		for (ShardNodeInformation si : nodeProvider.get()) {
			out.addAll(si.getJobManagerStatus());
		}

		return out;
	}

	public Object executeScript(String script) {

		final CompilerConfiguration config = new CompilerConfiguration();
		final GroovyShell groovy = new GroovyShell(config);

		groovy.setProperty("managerNode", getManagerNode());
		groovy.setProperty("datasetRegistry", getDatasetRegistry());
		groovy.setProperty("jobManager", getJobManager());
		groovy.setProperty("conqueryConfig", getConfig());
		groovy.setProperty("storage", getStorage());

		try {
			return groovy.evaluate(script);
		}
		catch (Exception e) {
			return ExceptionUtils.getStackTrace(e);
		}
	}

	public Set<IndexKey> getLoadedIndexes() {
		return datasetRegistry.getLoadedIndexes();
	}

	public CacheStats getIndexServiceStatistics() {
		return datasetRegistry.getIndexServiceStatistics();
	}

	public void resetIndexService() {
		log.info("Resetting index service");
		datasetRegistry.resetIndexService();
	}
}
