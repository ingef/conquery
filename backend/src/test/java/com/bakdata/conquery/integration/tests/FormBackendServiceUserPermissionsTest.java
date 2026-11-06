package com.bakdata.conquery.integration.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.permissions.WildcardPermission;
import com.bakdata.conquery.models.auth.util.SubjectPrincipalCollection;
import com.bakdata.conquery.models.config.FormBackendConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.util.AuthUtil;
import com.bakdata.conquery.util.support.StandaloneSupport;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.Permission;

public class FormBackendServiceUserPermissionsTest extends IntegrationTest.Simple implements ProgrammaticIntegrationTest {

	private static final String FORM_PERMISSION = "forms:CREATE:configured-form";
	private static final String QUERY_PERMISSION = "queries:READ:configured-query";
	private static final String UNCONFIGURED_PERMISSION = "forms:CREATE:other-form";

	@Override
	public void execute(StandaloneSupport conquery) {
		final MetaStorage storage = conquery.getMetaStorage();
		final Dataset dataset = conquery.getNamespace().getDataset();
		final TestUser originalUser = new TestUser(storage);
		storage.addUser(originalUser);

		final ConqueryPermission originalPermission = DatasetPermission.onInstance(Ability.READ, dataset.getId());
		originalUser.addPermission(originalPermission);
		final Set<ConqueryPermission> originalPermissions = Set.copyOf(originalUser.getPermissions());

		final FormBackendConfig formBackendConfig = new FormBackendConfig();
		formBackendConfig.setId("permission-test");
		formBackendConfig.setManagerNode(conquery.getStandaloneCommand().getManagerNode());
		formBackendConfig.setServiceUserPermissions(List.of(FORM_PERMISSION, QUERY_PERMISSION));

		User serviceUser = null;
		try {
			serviceUser = formBackendConfig.createServiceUser(originalUser, dataset);

			assertThat(serviceUser.getPermissions()).contains(originalPermission);
			assertThat(isPermitted(serviceUser, new WildcardPermission(FORM_PERMISSION))).isTrue();
			assertThat(isPermitted(serviceUser, new WildcardPermission(QUERY_PERMISSION))).isTrue();
			assertThat(isPermitted(serviceUser, new WildcardPermission(UNCONFIGURED_PERMISSION))).isFalse();
			assertThat(serviceUser.isPermitted(dataset, Ability.DOWNLOAD)).isTrue();

			assertThat(originalUser.getPermissions()).containsExactlyInAnyOrderElementsOf(originalPermissions);
			assertThat(isPermitted(originalUser, new WildcardPermission(FORM_PERMISSION))).isFalse();
			assertThat(isPermitted(originalUser, new WildcardPermission(QUERY_PERMISSION))).isFalse();
			assertThat(originalUser.isPermitted(dataset, Ability.DOWNLOAD)).isFalse();
		}
		finally {
			if (serviceUser != null) {
				AuthUtil.cleanUpUserAndBelongings(serviceUser, storage, conquery.getDatasetRegistry());
			}
			storage.removeUser(originalUser.getId());
		}
	}

	private static boolean isPermitted(User user, Permission permission) {
		return SecurityUtils.getSecurityManager().isPermitted(new SubjectPrincipalCollection(user, null), permission);
	}
}
