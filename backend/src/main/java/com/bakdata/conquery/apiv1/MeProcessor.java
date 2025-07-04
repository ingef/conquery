package com.bakdata.conquery.apiv1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.inject.Inject;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.resources.api.MeResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * This class holds the logic to back the endpoints provided by {@link MeResource}.
 */
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MeProcessor {

	@Inject
	private MetaStorage storage;
	@Inject
	private DatasetRegistry<? extends Namespace> datasetRegistry;

	/**
	 * Generates a summary of a user. It contains its name, the groups it belongs to and its permissions on a dataset.
	 *
	 * @param user The user object to gather informations about
	 * @return The information about the user
	 */
	public FrontendMeInformation getUserInformation(@NonNull User user) {
		// Compute dataset ablilities
		Map<DatasetId, FrontendDatasetAbility> datasetAblilites = new HashMap<>();
		for (DatasetId dataset : datasetRegistry.getAllDatasets().toList()) {
			if (!user.isPermitted(dataset, Ability.READ)) {
				// User is not allowed to use dataset
				continue;
			}

			// User can use the dataset and can possibly upload ids for resolving
			datasetAblilites.put(
					dataset,
					new FrontendDatasetAbility(
							user.isPermitted(dataset, Ability.PRESERVE_ID),
							user.isPermitted(dataset, Ability.ENTITY_PREVIEW) && user.isPermitted(dataset, Ability.PRESERVE_ID),
							user.isPermitted(dataset, Ability.QUERY_PREVIEW)
					)
			);
		}

		// Build user information
		return FrontendMeInformation.builder()
									.userName(user.getLabel())
									.hideLogoutButton(!user.isDisplayLogout())
									.groups(
											AuthorizationHelper.getGroupsOf(user, storage)
															   .stream()
															   .map(g -> new IdLabel<GroupId>(g.getId(), g.getLabel()))
															   .collect(Collectors.toList()))
									.datasetAbilities(datasetAblilites)
									.build();
	}

	/**
	 * Front end (API) data container to describe a single user.
	 */
	@Data
	@Builder
	public static class FrontendMeInformation {
		String userName;
		boolean hideLogoutButton;
		Map<DatasetId, FrontendDatasetAbility> datasetAbilities;
		List<IdLabel<GroupId>> groups;
	}

	/**
	 * Front end container to describe what the user can do on a
	 * dataset by simple flags.
	 */
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class FrontendDatasetAbility {
		private boolean canUpload;
		private boolean canViewEntityPreview;
		private boolean canViewQueryPreview;
	}

}
