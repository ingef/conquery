package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.auth.permissions.Authorized;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public interface Owned extends Authorized {
    UserId getOwner();
}
