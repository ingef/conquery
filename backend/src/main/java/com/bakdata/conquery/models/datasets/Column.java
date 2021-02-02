package com.bakdata.conquery.models.datasets;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.serializer.NsIdRef;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.parser.MajorTypeId;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Column extends Labeled<ColumnId> {

	public static final int UNKNOWN_POSITION = -1;

	@JsonBackReference
	@NotNull
	@ToString.Exclude
	private Table table;
	@NotNull
	private MajorTypeId type;

	@InternalOnly
	private int position = UNKNOWN_POSITION;
	/**
	 * if set this column should use the given dictionary
	 * if it is of type string, instead of its own dictionary
	 */
	private String sharedDictionary;
	/**
	 * if this is set this column counts as the secondary id of the given name for this
	 * table
	 */
	@NsIdRef
	private SecondaryIdDescription secondaryId;

	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}

	public boolean matches(PPColumn column) {
		if (!this.getName().equals(column.getName())) {
			return false;
		}
		return this.getType().equals(column.getType());
	}

	public ColumnStore getTypeFor(Bucket bucket) {
		return bucket.getStores()[getPosition()];
	}


	//TODO try to remove this method methods, they are quite leaky
	public ColumnStore getTypeFor(Import imp) {
		if (!imp.getTable().equals(getTable().getId())) {
			throw new IllegalArgumentException(String.format("Import %s is not for same table as %s", imp.getTable(), getTable().getId()));
		}

		return imp.getColumns()[getPosition()].getTypeDescription();
	}

	@Override
	public String toString() {
		return String.format("Column[%s](type = %s)", getId(), getType());
	}
}
