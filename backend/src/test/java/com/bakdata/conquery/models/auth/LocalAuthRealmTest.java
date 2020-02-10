package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationConfig;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.TokenHandler;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.util.LifecycleUtils;
import org.junit.jupiter.api.Test;

/**
 * Tests the basic functionality of the LocalAuthenticationRealm, which stores
 * the credential data in a local database and offers simple user management
 * (add/update/delete).
 */
public class LocalAuthRealmTest {

	@Test
	public void test() {
		LocalAuthenticationConfig config = new LocalAuthenticationConfig();
		config.setTokenSecret("TestTokenSecret");

		MasterMetaStorage storage = mock(MasterMetaStorage.class);
		LocalAuthenticationRealm realm = config.createRealm(storage);
		File tmpDir = Files.createTempDir();

		tmpDir.mkdir();

		ConqueryConfig.getInstance().getStorage().setDirectory(tmpDir);

		LifecycleUtils.init(realm);

		// Create User in Realm
		User user1 = new User("TestUser", "Test User");
		PasswordCredential user1Password = new PasswordCredential(new String("testPassword").toCharArray());
		realm.addUser(user1, List.of(user1Password));
		// And mock him into the storage
		when(storage.getUser(user1.getId())).thenReturn(user1);

		// Check Credentials
		String jwt = null;
		{
			// Empty username
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("", new String("testPassword").toCharArray()))
				.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Username was empty");

			// Empty password
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("").toCharArray()))
				.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Password was empty");

			// Wrong password
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("wrongPassword").toCharArray()))
				.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

			// Wrong username
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("NoTestUser", new String("testPassword").toCharArray()))
				.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

			// Right username and password should yield a JWT
			jwt = realm.checkCredentialsAndCreateJWT("TestUser", new String("testPassword").toCharArray());
			assertThat(jwt).matches(TokenHandler.JWT_PATTERN);
		}

		assertThat(realm.doGetAuthenticationInfo(new JwtToken(jwt)).getPrincipals().getPrimaryPrincipal())
			.isEqualTo(new UserId("TestUser"));

		// Check password update
		{
			realm.updateUser(user1, List.of(new PasswordCredential(new String("newTestPassword").toCharArray())));
			// Wrong password
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("testPassword").toCharArray()))
				.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

			jwt = realm.checkCredentialsAndCreateJWT("TestUser", new String("newTestPassword").toCharArray());
			assertThat(jwt).matches(TokenHandler.JWT_PATTERN);
		}

		// Check user delete (from realm only)
		{
			realm.removeUser(user1);
			// Wrong password
			assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("newTestPassword").toCharArray()))
				.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");
		}

		FileUtils.deleteQuietly(tmpDir);
	}
}
