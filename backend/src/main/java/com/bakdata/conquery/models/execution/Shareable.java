package com.bakdata.conquery.models.execution;

import java.util.function.Consumer;
import java.util.function.Function;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.entities.PermissionOwner;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.identifiable.Identifiable;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.resources.api.StoredQueriesResource.QueryPatch;
import com.bakdata.conquery.util.QueryUtils;

public interface Shareable {
	static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Shareable.class);
	
	boolean isShared();
	void setShared(boolean shared);
	
	
	default  <S extends Identifiable<?> & Shareable, O extends PermissionOwner<? extends IId<O>>> Consumer<QueryPatch> sharer(MasterMetaStorage storage, User user, O other, Function<S,ConqueryPermission> sharedPermissionCreator) {
		if(!(this instanceof Identifiable<?>)) {
			log.warn("Cannot share {} ({}) because it does not implement Identifiable", this.getClass(), this.toString());
			return QueryUtils.getNoOpEntryPoint();
		}
		return (patch) -> {
			QueryUtils.shareWithOther(
				storage,
				user,
				(S) this,
				sharedPermissionCreator, 
				other,
				patch.getShared());
		};
		
	}
}
