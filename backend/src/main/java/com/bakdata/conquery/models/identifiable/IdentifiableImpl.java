package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
public abstract class IdentifiableImpl<ID extends Id<? extends Identifiable<? extends ID>>> implements Identifiable<ID> {

	@JsonIgnore
	protected transient ID cachedId;
	@JsonIgnore
	private transient int cachedHash = Integer.MIN_VALUE;

	@ToString.Include
	@JsonIgnore
	@Override
	public ID getId() {
		if (cachedId == null) {
			cachedId = IdUtil.intern(createId());
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
			if (other.getId() != null) {
				return false;
			}
		} else if (!getId().equals(other.getId())) {
			return false;
		}
		return true;
	}
}
