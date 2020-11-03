package com.bakdata.conquery.apiv1;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.QueryPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.shiro.authz.Permission;

/**
 * API-Class that holds exactly the informations that the front end needs when querying an stored query.
 * 
 * @implNote It can be tedious to maintain several Api classes for different end points that have
 * overlapping information. GraphQL might be a solution for this.
 * 
 * NOT USED AT THE MOMENT
 */
@AllArgsConstructor
@Getter
public class StoredQuerySingleInfo {
	private ManagedExecutionId id;
	private String label;
	private ZonedDateTime createdAt; // ISO timestamp: 2019-06-18T11:11:50.528626+02:00
	private boolean own;
	private boolean shared;
	private boolean system;
	@JsonProperty("isPristineLabel")
	private boolean isPristineLabel;
	private String ownerName;
	private URL resultUrl;
	private Collection<IdLabel<GroupId>> groups;
	private QueryDescription query;
	private String[] tags;
	private Long numberOfResults;
	
	public static StoredQuerySingleInfo from(ManagedQuery query, User user, MetaStorage metaStorage,  UriBuilder url) {
		/* Calculate which groups can see this query.
		 * This usually is usually not done very often and should be reasonable fast, so don't cache this.
		 */
		List<IdLabel<GroupId>> permittedGroups = new ArrayList<>();
		for(Group group : metaStorage.getAllGroups()) {
			for(Permission perm : group.getPermissions()) {
				if(perm.implies(QueryPermission.onInstance(Ability.READ, query.getId()))) {
					permittedGroups.add(new IdLabel<GroupId>(group.getId(), group.getLabel()));
					continue;
				}
			}
		}
		
		return new StoredQuerySingleInfo(
			query.getId(), 
			query.getLabel(),
			query.getCreationTime().atZone(ZoneId.systemDefault()),
			query.getOwner().equals(user.getId()),
			query.isShared(),
			false, // there is no mechanism/definition yet for system queries
			query.getLabel() == null,
			Optional.ofNullable(query.getOwner()).map(owner -> metaStorage.getUser(owner)).map(User::getLabel).orElse(null),
			query.getDownloadURL(url, user).orElse(null),
			permittedGroups,
			query.getQuery(),
			query.getTags(),
			query.getLastResultCount()
			);
	}
}
