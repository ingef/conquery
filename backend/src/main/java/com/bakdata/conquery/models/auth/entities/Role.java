package com.bakdata.conquery.models.auth.entities;

import java.util.function.Consumer;

import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class Role extends PermissionOwner<RoleId> {
	

	@Override
	public RoleId createId() {
		return new RoleId(name);
	}

	@JacksonInject(useInput = OptBoolean.FALSE)
	@NonNull
	@EqualsAndHashCode.Exclude
	protected StorageUpdater storageUpdater;

	public Role(String name, String label, Consumer<Role> storageUpdater) {
		super(name, label);
		this.storageUpdater = new StorageUpdater(storageUpdater);
	}

	@Override
	protected void updateStorage() {
		storageUpdater.accept(this);

	}



	@RequiredArgsConstructor
	public static class StorageUpdater implements MetaStorage.StorageUpdater<Role> {

		private final Consumer<Role> storageUpdater;

		@Override
		public void accept(Role role) {
			storageUpdater.accept(role);
		}

		@Override
		public MutableInjectableValues inject(MutableInjectableValues values) {
			values.add(StorageUpdater.class, this);
			return values;
		}
	}

}
