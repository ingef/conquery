package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
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
	private final DatasetRegistry datasetRegistry;

	/**
	 * Generates a summary of a user. It contains its name, the groups it belongs to and its permissions on a dataset.
	 * @param user The user object to gather informations about
	 * @return The information about the user
	 */
	public FEMeInformation getUserInformation(@NonNull User user){
		// Compute dataset ablilities
		Map<DatasetId, FEDatasetAbility> datasetAblilites= new HashMap<>();
		for(Dataset dataset : datasetRegistry.getAllDatasets()){
			boolean[] result = user.isPermitted(List.of(
					DatasetPermission.onInstance(Ability.READ, dataset.getId()),
					DatasetPermission.onInstance(Ability.PRESERVE_ID, dataset.getId())
			));

			if(!result[0] /* READ */) {
				// User is not allowed to use dataset
				continue;
			}

			// User can use the dataset and can possibly upload ids for resolving
			datasetAblilites.put(
					dataset.getId(),
					FEDatasetAbility.builder()
							.canUpload(result[1] /*PRESERVE_ID*/)
							.build());
		}

		// Build user information
		return FEMeInformation.builder()
				.userName(user.getLabel())
				.hideLogoutButton(!user.isDisplayLogout())
				.groups(
						AuthorizationHelper.getGroupsOf(user.getId(), storage)
								.stream()
								.map(g -> new IdLabel<GroupId>(g.getId(),g.getLabel()))
								.collect(Collectors.toList()))
				.datasetAbilities(datasetAblilites)
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
		Map<DatasetId, FEDatasetAbility> datasetAbilities;
		List<IdLabel<GroupId>> groups;
	}

	/**
	 * Front end container to describe what the user can do on a
	 * dataset by simple flags.
	 */
	@Builder
	private static class FEDatasetAbility {
		private final boolean canUpload;
	}

}
