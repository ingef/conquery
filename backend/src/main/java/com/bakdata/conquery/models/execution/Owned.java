package com.bakdata.conquery.models.execution;

import com.bakdata.conquery.models.identifiable.ids.specific.UserId;

public interface Owned {
    UserId getOwner();
}
