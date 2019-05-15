package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.PermissionOwner;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper=false)
public class MandatorId extends PermissionOwnerId<Mandator> {
	public static final String TYPE = "mandator";
	
	@Getter
	private final String mandator;
	
	public MandatorId(String mandator) {
		super();
		this.mandator = mandator;
	}

	public void collectComponents(List<Object> components) {
		super.collectComponents(components);
		components.add(TYPE);
		components.add(mandator);
	}
	
	enum Parser implements IId.Parser<MandatorId> {
		INSTANCE;
		
		@Override
		public MandatorId parseInternally(IdIterator parts) {
			return (MandatorId) PermissionOwnerId.Parser.INSTANCE.parse(parts);
		}
	}

	@Override
	public PermissionOwner<?> getOwner(MasterMetaStorage storage) {
		return storage.getMandator(this);
	}
}
