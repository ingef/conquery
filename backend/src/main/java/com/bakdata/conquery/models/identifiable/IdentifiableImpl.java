package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.jackson.serializer.IdDeserializer;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.*;

@NoArgsConstructor
public abstract class IdentifiableImpl<ID extends Id<? extends Identifiable<? extends ID>>> implements Identifiable<ID> {

	@JsonIgnore
	protected transient ID cachedId;
	@JsonIgnore
	private transient int cachedHash = Integer.MIN_VALUE;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@JsonIgnore
	private transient MetaStorage metaStorage;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@JsonIgnore
	private transient NsIdResolver nsIdResolver;

	@ToString.Include
	@JsonIgnore
	@Override
	public ID getId() {
		if (cachedId == null) {
			final ID intern = createId();
			// Set resolver
			IdDeserializer.setResolver(intern, metaStorage, nsIdResolver);

			cachedId = intern;
		}
		return cachedId;
	}
	
	public abstract ID createId();
	
	
	@Override
	public String toString() {
		return this.getClass().getSimpleName()+"["+ getId() + "]";
	}
	
	@Override
	public int hashCode() {
		if(cachedHash == Integer.MIN_VALUE) {
			int result = 1;
			result = 31 * result + ((getId() == null) ? 0 : getId().hashCode());
			cachedHash = result;
		}
		return cachedHash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		IdentifiableImpl<?> other = (IdentifiableImpl<?>) obj;
		if (getId() == null) {
			return other.getId() == null;
		}
		else {
			return getId().equals(other.getId());
		}
	}
}
