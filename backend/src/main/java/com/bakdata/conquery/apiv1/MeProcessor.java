package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
public class MeProcessor {

	private final MasterMetaStorage storage;
	
	public FEMeInformation getUserInformation(User user){
		return FEMeInformation.builder()
			.userName(user.getLabel())
			.groups(FEGroup.from(AuthorizationHelper.getGroupsOf(user, storage)))
			.permissions( FEPermission.from(AuthorizationHelper.getEffectiveUserPermissions(user.getId(), List.of(DatasetPermission.DOMAIN), storage)))
			.build();
	}

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Getter
	public static class FEGroup {

		private GroupId groupId;
		private String label;

		public static FEGroup from(Group group) {
			return new FEGroup(group.getId(), group.getLabel());
		}
		
		public static List<FEGroup> from(List<Group> groups) {
			return groups.stream().map(FEGroup::from).collect(Collectors.toList());
		}
	}
	
	@Data
	@Builder
	public static class FEMeInformation {
		String userName;
		List<FEPermission> permissions;
		List<FEGroup> groups;
	}

}
