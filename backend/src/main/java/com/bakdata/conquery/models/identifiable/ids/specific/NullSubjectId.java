package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.NullSubject;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.google.common.collect.PeekingIterator;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=false)
public class NullSubjectId extends PermissionOwnerId<NullSubject> {
	public static final String TYPE = "nullType";
	public static final String NAME = "NullSubject";
	public static final String REALM = "nonExistingRealm";
	
	@Override
	public void collectComponents(List<Object> components) {
		super.collectComponents(components);
		components.add(TYPE);
		components.add(NAME);
	}
	
	enum Parser implements IId.Parser<NullSubjectId> {
		INSTANCE;
		
		@Override
		public NullSubjectId parse(PeekingIterator<String> parts) {
			throw new UnsupportedOperationException("This type of Id is not intented to be serialized or deserialized");
		}
	}

	@Override
	public PermissionOwner<?> getOwner(MasterMetaStorage storage) {
		throw new UnsupportedOperationException(this.getClass().getSimpleName() + " is not stored and can not be retrieved from a storage.");
	}
}
