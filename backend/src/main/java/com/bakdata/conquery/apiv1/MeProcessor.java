package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import com.bakdata.conquery.resources.api.MeResource;
import lombok.*;

/**
 * This class holds the logic to back the endpoints provided by {@link MeResource}.
 */
@AllArgsConstructor
@ToString
public class MeProcessor {

	private final MetaStorage storage;

	/**
	 * Generates a summary of a user. It contains its name, the groups it belongs to and its permissions on a dataset.
	 * @param user The user object to gather informations about
	 * @return The information about the user
	 */
	public FEMeInformation getUserInformation(@NonNull User user){
		return FEMeInformation.builder()
				.userName(user.getLabel())
				.hideLogoutButton(!user.isDisplayLogout())
				.groups(
						AuthorizationHelper.getGroupsOf(user.getId(), storage)
								.stream()
								.map(g -> new IdLabel<GroupId>(g.getId(),g.getLabel()))
								.collect(Collectors.toList()))
				.permissions( FEPermission.from(AuthorizationHelper.getEffectiveUserPermissions(user.getId(), List.of(DatasetPermission.DOMAIN), storage).values()))
				.build();
	}

	/**
	 * Front end (API) data container to describe a single user.
	 */
	@Data
	@Builder
	public static class FEMeInformation {
		String userName;
		boolean hideLogoutButton;
		List<FEPermission> permissions;
		List<IdLabel<GroupId>> groups;
	}

}
