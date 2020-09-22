package com.bakdata.conquery.apiv1;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.shiro.authz.Permission;

@AllArgsConstructor
@Getter
public class StoredQueryItem {
	private ManagedExecutionId id;
	private String label;
	private ZonedDateTime createdAt; // ISO timestamp: 2019-06-18T11:11:50.528626+02:00
	private boolean own;
	private boolean shared;
	private boolean system;
	private boolean isPristineLabel,
	private String ownerName;
	private Long numberOfResults;
	private URL resultUrl;
	private String[] tags;
	private QueryDescription query;
	private Collection<IdLabel<GroupId>> groups;
	
	public static StoredQueryItem from(ManagedQuery execution, User user, MetaStorage metaStorage,  URLBuilder url) {
		List<IdLabel<GroupId>> permittedGroups = new ArrayList<>();
		for(Group group : metaStorage.getAllGroups()) {
			for(Permission perm : group.getPermissions()) {
				if(perm.implies(QueryPermission.onInstance(Ability.READ, execution.getId()))) {
					permittedGroups.add(new IdLabel<GroupId>(group.getId(), group.getLabel()));
				}
			}
		}
		return new StoredQueryItem(
			execution.getId(), 
			execution.getLabel(),
			execution.getCreationTime().atZone(ZoneId.systemDefault()),
			execution.getOwner().equals(user.getId()),
			execution.isShared(),
			false, // there is no mechanism/definition yet for system queries
			execution.getLabel() == null,
			Optional.ofNullable(execution.getOwner()).map(owner -> metaStorage.getUser(owner)).map(User::getLabel).orElse(null),
			execution.getLastResultCount(),
			execution.isReadyToDownload(url, user) ? execution.getDownloadURL(url) : null,
			execution.getTags(),
			execution.getQuery(),
			permittedGroups);
	}
}
