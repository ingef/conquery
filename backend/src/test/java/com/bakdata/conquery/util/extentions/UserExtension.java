package com.bakdata.conquery.util.extentions;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.auth.entities.User;
import lombok.Getter;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class UserExtension implements BeforeAllCallback {
	private final MetaStorage metaStorage;

	@Getter
	private final User user;

	public UserExtension(MetaStorage metaStorage, int id) {
		this.metaStorage = metaStorage;
		user = new User("user" + id, null);
	}

	@Override
	public void beforeAll(ExtensionContext extensionContext) throws Exception {

		user.setMetaStorage(metaStorage);
		metaStorage.addUser(user);
	}
}
