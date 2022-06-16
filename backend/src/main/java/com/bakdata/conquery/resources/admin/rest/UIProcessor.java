package com.bakdata.conquery.resources.admin.rest;

import com.bakdata.conquery.io.cps.CPSTypeIdResolver;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NamespaceStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.Role;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.StringPermissionBuilder;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.datasets.concepts.Concept;
import com.bakdata.conquery.models.datasets.concepts.tree.ConceptTreeNode;
import com.bakdata.conquery.models.datasets.concepts.tree.TreeConcept;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.resources.admin.ui.model.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper processor that transforms internal representations of the {@link AdminProcessor} into
 * objects that are more convenient to handle with freemarker.
 */
@RequiredArgsConstructor
@Slf4j
public class UIProcessor {

	@Getter
	private final AdminProcessor adminProcessor;

	public DatasetRegistry getDatasetRegistry() {
		return adminProcessor.getDatasetRegistry();
	}

	public MetaStorage getStorage() {
		return adminProcessor.getStorage();
	}

	public UIContext getUIContext() {
		return new UIContext(getDatasetRegistry());
	}

	public FEAuthOverview getAuthOverview() {
		Collection<FEAuthOverview.OverviewRow> overview = new TreeSet<>();
		for (User user : getStorage().getAllUsers()) {
			Collection<Group> userGroups = AuthorizationHelper.getGroupsOf(user, getStorage());
			Set<Role> effectiveRoles = user.getRoles().stream().map(getStorage()::getRole).collect(Collectors.toSet());
			userGroups.forEach(g -> effectiveRoles.addAll(g.getRoles().stream().map(getStorage()::getRole).sorted().collect(Collectors.toList())));
			overview.add(FEAuthOverview.OverviewRow.builder().user(user).groups(userGroups).effectiveRoles(effectiveRoles).build());
		}

		return FEAuthOverview.builder().overview(overview).build();
	}


	public FERoleContent getRoleContent(Role role) {
		return FERoleContent.builder()
				.permissions(wrapInFEPermission(role.getPermissions()))
				.permissionTemplateMap(preparePermissionTemplate())
				.users(getUsers(role))
				.groups(getGroups(role))
				.owner(role)
				.build();
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
			} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
				log.error("Could not access allowed abilities for permission type: {}", permissionType, e);
			}

		}
		return permissionTemplateMap;
	}

	public List<User> getUsers(Role role) {
		Collection<User> user = getStorage().getAllUsers();
		return user.stream().filter(u -> u.getRoles().contains(role.getId())).sorted().collect(Collectors.toList());
	}

	private List<Group> getGroups(Role role) {
		Collection<Group> groups = getStorage().getAllGroups();
		return groups.stream()
					 .filter(g -> g.getRoles().contains(role.getId()))
					 .sorted()
					 .collect(Collectors.toList());
	}

	private SortedSet<FEPermission> wrapInFEPermission(Collection<ConqueryPermission> permissions) {
		TreeSet<FEPermission> fePermissions = new TreeSet<>();

		for (ConqueryPermission permission : permissions) {
			fePermissions.add(FEPermission.from(permission));
		}
		return fePermissions;
	}

	public FEUserContent getUserContent(User user) {
		final Collection<Group> availableGroups = new ArrayList<>(getStorage().getAllGroups());
		availableGroups.removeIf(g -> g.containsMember(user));

		return FEUserContent
				.builder()
				.owner(user)
				.groups(AuthorizationHelper.getGroupsOf(user, getStorage()))
				.availableGroups(availableGroups)
				.roles(user.getRoles().stream().map(getStorage()::getRole).collect(Collectors.toList()))
				.availableRoles(getStorage().getAllRoles())
				.permissions(wrapInFEPermission(user.getPermissions()))
				.permissionTemplateMap(preparePermissionTemplate())
				.build();
	}


	public FEGroupContent getGroupContent(Group group) {

		Set<UserId> membersIds = group.getMembers();
		ArrayList<User> availableMembers = new ArrayList<>(getStorage().getAllUsers());
		availableMembers.removeIf(u -> membersIds.contains(u.getId()));
		return FEGroupContent
				.builder()
				.owner(group)
				.members(membersIds.stream().map(getStorage()::getUser).collect(Collectors.toList()))
				.availableMembers(availableMembers)
				.roles(group.getRoles().stream().map(getStorage()::getRole).collect(Collectors.toList()))
				.availableRoles(getStorage().getAllRoles())
				.permissions(wrapInFEPermission(group.getPermissions()))
				.permissionTemplateMap(preparePermissionTemplate())
				.build();
	}

	public TableStatistics getTableStatistics(Table table) {
		final NamespaceStorage storage = getDatasetRegistry().get(table.getDataset().getId()).getStorage();
		List<Import> imports = table.findImports(storage).collect(Collectors.toList());

		final long entries = imports.stream().mapToLong(Import::getNumberOfEntries).sum();

		return new TableStatistics(
				table,
				entries,
				//total size of dictionaries
				imports.stream()
						.flatMap(imp -> imp.getDictionaries().stream())
						.filter(Objects::nonNull)
						.map(storage::getDictionary)
						.mapToLong(Dictionary::estimateMemoryConsumption)
						.sum(),
				//total size of entries
				imports.stream()
						.mapToLong(Import::estimateMemoryConsumption)
						.sum(),
				// Total size of CBlocks
				imports.stream()
						.mapToLong(imp -> calculateCBlocksSizeBytes(imp, storage.getAllConcepts()))
						.sum(),
				imports

		);
	}

	public ImportStatistics getImportStatistics(Import imp) {
		final NamespaceStorage storage = getDatasetRegistry().get(imp.getDataset().getId()).getStorage();

		final long cBlockSize = calculateCBlocksSizeBytes(imp, storage.getAllConcepts());

		return new ImportStatistics(imp, cBlockSize);
	}

	public static long calculateCBlocksSizeBytes(Import imp, Collection<? extends Concept<?>> concepts) {

		// CBlocks are created per (per Bucket) Import per Connector targeting this table
		// Since the overhead of a single CBlock is minor, we gloss over the fact, that there are multiple and assume it is only a single very large one.
		return concepts.stream()
				.filter(TreeConcept.class::isInstance)
				.flatMap(concept -> ((TreeConcept) concept).getConnectors().stream())
				.filter(con -> con.getTable().equals(imp.getTable()))
				.mapToLong(con -> {
					// Per event an int array is stored marking the path to the concept child.
					final double avgDepth = con.getConcept()
							.getAllChildren()
							.mapToInt(ConceptTreeNode::getDepth)
							.average()
							.orElse(1d);

					return CBlock.estimateMemoryBytes(imp.getNumberOfEntities(), imp.getNumberOfEntries(), avgDepth);
				})
				.sum();
	}
}
