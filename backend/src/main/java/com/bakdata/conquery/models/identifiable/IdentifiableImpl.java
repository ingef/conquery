package com.bakdata.conquery.models.identifiable;

import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.io.storage.NsIdResolver;
import com.bakdata.conquery.models.identifiable.ids.Id;
import com.bakdata.conquery.models.identifiable.ids.IdUtil;
import com.bakdata.conquery.models.identifiable.ids.NamespacedId;
import com.bakdata.conquery.models.identifiable.ids.specific.WorkerId;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
public abstract class IdentifiableImpl<ID extends Id<? extends Identifiable<? extends ID>>> implements Identifiable<ID> {

	@JsonIgnore
	protected transient ID cachedId;
	@JsonIgnore
	private transient int cachedHash = Integer.MIN_VALUE;

	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@Getter(AccessLevel.PROTECTED)
	@EqualsAndHashCode.Exclude
	private transient MetaStorage metaStorage;
	@JacksonInject(useInput = OptBoolean.FALSE)
	@Setter
	@EqualsAndHashCode.Exclude
	private transient NsIdResolver nsIdResolver;

	@ToString.Include
	@JsonIgnore
	@Override
	public ID getId() {
		if (cachedId == null) {
			cachedId = IdUtil.intern(createId());

			// Set resolver
			if (cachedId instanceof NamespacedId) {
				Preconditions.checkState(nsIdResolver != null);
				cachedId.setIdResolver(() -> nsIdResolver.get((Id<?> & NamespacedId) cachedId));
			}
			else if (cachedId instanceof WorkerId) {
				// TODO WorkerIds are not resolved yet
			}
			else {
				Preconditions.checkState(metaStorage != null);
				cachedId.setIdResolver(() -> metaStorage.get(cachedId));
			}
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
