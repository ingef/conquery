package com.bakdata.conquery.models.datasets;

import javax.validation.constraints.NotNull;

import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.identifiable.Labeled;
import com.bakdata.conquery.models.identifiable.ids.specific.ColumnId;
import com.bakdata.conquery.models.identifiable.ids.specific.SecondaryIdId;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.types.CType;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Column extends Labeled<ColumnId> {

	public static final int UNKNOWN_POSITION = -1;
	public static final int PRIMARY_POSITION = -2;

	@JsonBackReference
	@NotNull
	@ToString.Exclude
	private Table table;
	@NotNull
	private MajorTypeId type;
	@JsonIgnore
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
	private SecondaryIdId secondaryId;

	@Override
	public ColumnId createId() {
		return new ColumnId(table.getId(), getName());
	}

	public boolean matches(PPColumn column) {
		if (!this.getName().equals(column.getName())) {
			return false;
		}
		return this.getType().equals(column.getType().getTypeId());
	}

	public CType getTypeFor(Bucket bucket) {
		return getTypeFor(bucket.getImp());
	}

	public CType getTypeFor(Import imp) {
		if (!imp.getTable().equals(getTable().getId())) {
			throw new IllegalArgumentException(String.format("Import %s is not for same table as %s", imp.getTable(), getTable().getId()));
		}

		return imp.getColumns()[getPosition()].getType();
	}

	public int getPosition() {
		if (position == UNKNOWN_POSITION) {
			if (table.getPrimaryColumn() == this) {
				position = PRIMARY_POSITION;
			}
			else {
				for (int i = 0; i < table.getColumns().length; i++) {
					if (table.getColumns()[i] == this) {
						position = i;
						break;
					}
				}
			}

			if (position == UNKNOWN_POSITION) {
				throw new IllegalStateException("Could not find the position of column" + this.getId() + " in its table");
			}
		}
		return position;
	}

	@Override
	public String toString() {
		return String.format("Column[%s](type = %s)",getId(), getType());
	}
}
