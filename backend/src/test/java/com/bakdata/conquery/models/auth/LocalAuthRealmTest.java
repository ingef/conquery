package com.bakdata.conquery.models.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import com.auth0.jwt.JWT;
import com.bakdata.conquery.apiv1.auth.PasswordCredential;
import com.bakdata.conquery.io.xodus.MetaStorage;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationConfig;
import com.bakdata.conquery.models.auth.basic.LocalAuthenticationRealm;
import com.bakdata.conquery.models.auth.basic.TokenHandler.JwtToken;
import com.bakdata.conquery.models.auth.develop.DevelopmentAuthorizationConfig;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.google.common.collect.MoreCollectors;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Tests the basic functionality of the LocalAuthenticationRealm, which stores
 * the credential data in a local database and offers simple user management
 * (add/update/delete).
 */
@TestInstance(Lifecycle.PER_CLASS)
public class LocalAuthRealmTest {

	private File tmpDir;
	private AuthorizationController controller;
	private LocalAuthenticationRealm realm;
	private MetaStorage storage;
	private User user1;

	@BeforeAll
	public void setupAll() throws Exception {
		LocalAuthenticationConfig config = new LocalAuthenticationConfig();

		storage = mock(MetaStorage.class);
		File tmpDir = Files.createTempDir();

		tmpDir.mkdir();

		ConqueryConfig.getInstance().getStorage().setDirectory(tmpDir);
		controller = new AuthorizationController(new DevelopmentAuthorizationConfig(),List.of(config), storage);
		controller.init();
		controller.start();
		realm = (LocalAuthenticationRealm) controller.getAuthenticationRealms().stream().filter(r -> r instanceof LocalAuthenticationRealm).collect(MoreCollectors.onlyElement());
	}

	@BeforeEach
	public void setupEach() {
		// Create User in Realm
		user1 = new User("TestUser", "Test User");
		PasswordCredential user1Password = new PasswordCredential(new String("testPassword").toCharArray());
		realm.addUser(user1, List.of(user1Password));
		// And mock him into the storage
		when(storage.getUser(user1.getId())).thenReturn(user1);
	}

	@AfterAll
	public void cleanUpEach() {
		// Well there is an extra test case for this, but lets do it like this for now.
		realm.removeUser(user1);
	}

	@AfterAll
	public void cleanUpAll() {

		FileUtils.deleteQuietly(tmpDir);

	}

	@Test
	public void testEmptyUsername() {
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("", new String("testPassword").toCharArray()))
			.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Username was empty");
	}

	@Test
	public void testEmptyPassword() {
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("").toCharArray()))
			.isInstanceOf(IncorrectCredentialsException.class).hasMessageContaining("Password was empty");
	}

	@Test
	public void testWrongPassword() {
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("wrongPassword").toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");
	}

	@Test
	public void testWrongUsername() {
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("NoTestUser", new String("testPassword").toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");
	}

	@Test
	public void testValidUsernamePassword() {
		// Right username and password should yield a JWT
		String jwt = realm.checkCredentialsAndCreateJWT("TestUser", new String("testPassword").toCharArray());
		assertThatCode(() -> JWT.decode(jwt)).doesNotThrowAnyException();

		assertThat(controller.getCentralTokenRealm().doGetAuthenticationInfo(new JwtToken(jwt)).getPrincipals().getPrimaryPrincipal())
			.isEqualTo(new UserId("TestUser"));
	}

	@Test
	public void testUserUpdate() {

		realm.updateUser(user1, List.of(new PasswordCredential(new String("newTestPassword").toCharArray())));
		// Wrong (old) password
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("testPassword").toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

		// Right (new) password
		String jwt = realm.checkCredentialsAndCreateJWT("TestUser", new String("newTestPassword").toCharArray());
		assertThatCode(() -> JWT.decode(jwt)).doesNotThrowAnyException();
	}

	@Test
	public void testRemoveUser() {
		realm.removeUser(user1);
		// Wrong password
		assertThatThrownBy(() -> realm.checkCredentialsAndCreateJWT("TestUser", new String("testPassword").toCharArray()))
			.isInstanceOf(AuthenticationException.class).hasMessageContaining("Provided username or password was not valid.");

	}
}
