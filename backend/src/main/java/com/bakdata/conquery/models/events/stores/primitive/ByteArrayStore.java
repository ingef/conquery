package com.bakdata.conquery.models.events.stores.primitive;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.ToString;


/**
 * Can store only 255 different values, the last one is reserved as NULL-flag.
 *
 * @apiNote do not instantiate this directly, but use {@link com.bakdata.conquery.models.preproc.parser.specific.IntegerParser}
 */
@CPSType(id = "BYTES", base = ColumnStore.class)
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class ByteArrayStore implements IntegerStore {

	private final byte nullValue;
	private final byte[] values;

	@JsonCreator
	public ByteArrayStore(byte[] values, byte nullValue) {
		this.nullValue = nullValue;
		this.values = values;
	}

	public static ByteArrayStore create(int size) {
		return new ByteArrayStore(new byte[size], Byte.MAX_VALUE);
	}

	@Override
	@ToString.Include
	public int getLines() {
		return values.length;
	}

	@Override
	public long estimateEventBits() {
		return Byte.SIZE;
	}

	public ByteArrayStore select(int[] starts, int[] ends) {
		return new ByteArrayStore(ColumnStore.selectArray(starts, ends, values, byte[]::new), getNullValue());
	}

	@Override
	public void setInteger(int event, long value) {
		values[event] = (byte) value;
	}

	@Override
	public boolean has(int event) {
		return values[event] != nullValue;
	}

	@Override
	public void setNull(int event) {
		values[event] = nullValue;
	}

	@Override
	public long getInteger(int event) {
		return values[event];
	}

}
