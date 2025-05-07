package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
public sealed abstract class IdentifiableImpl<ID extends Id<?, DOMAIN>, DOMAIN> implements Identifiable<ID, DOMAIN>
		permits NamespacedIdentifiable, MetaIdentifiable {

	@JsonIgnore
	protected transient ID cachedId;
	@JsonIgnore
	private transient int cachedHash = Integer.MIN_VALUE;


	@Override
	public int hashCode() {
		if (cachedHash == Integer.MIN_VALUE) {
			int result = 1;
			result = 31 * result + ((getId() == null) ? 0 : getId().hashCode());
			cachedHash = result;
		}
		return cachedHash;
	}

	@ToString.Include
	@JsonIgnore
	@Override
	public ID getId() {
		if (cachedId == null) {
			cachedId = createId();
			cachedId.setDomain(getDomain());
		}
		return cachedId;
	}

	public abstract ID createId();

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
		IdentifiableImpl<?, ?> other = (IdentifiableImpl<?, ?>) obj;
		if (getId() == null) {
			return other.getId() == null;
		}
		else {
			return getId().equals(other.getId());
		}
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[" + getId() + "]";
	}
}
