package com.bakdata.conquery.models.preproc.outputs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.common.CDate;
import com.bakdata.conquery.models.common.CDateRange;
import com.bakdata.conquery.models.common.QuarterUtils;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.preproc.ColumnDescription;
import com.bakdata.conquery.models.preproc.PPColumn;
import com.bakdata.conquery.models.preproc.outputs.daysinrange.DaysInRange;
import com.bakdata.conquery.models.preproc.outputs.daysinrange.Identifier;
import com.bakdata.conquery.models.preproc.outputs.daysinrange.PatientEvent;
import com.bakdata.conquery.models.preproc.outputs.daysinrange.TypeIdDaysInRangeMerger;
import com.bakdata.conquery.models.preproc.outputs.daysinrange.TypeIdDaysInRangeMerger.DaysInRangeEntry;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.primitives.Ints;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


//see #152  Move to EVA
@NoArgsConstructor
@Setter @Getter
@CPSType(id="DAYS_IN_RANGE", base=AutoOutput.class)
public class DaysInRangeOutput implements AutoOutput {

	public static final int CONSTANT_COLUMN_OFFSET = 4;
	@NotNull @Valid
	private List<Output> identifiers;
	@Min(0)
	private int yearColumn;
	private TypeId dayType;
	@Min(0)
	private int quarterColumn;
	@JsonIgnore
	private transient final Collection<PatientEvent> emitted = new ArrayList<>();
	@JsonIgnore
	private transient final TypeIdDaysInRangeMerger merger = TypeIdDaysInRangeMerger.builder()
																					.mergeCondition(DaysInRangeOutput::isFullQuarter)
																					.rangeFunction(DaysInRange::rangeFromStart)
																					.consumer(emitted::add)
																					.build();


	private static boolean isFullQuarter(DaysInRange current) {
		return QuarterUtils.isBeginOfQuarter(current.getStart()) || QuarterUtils.isEndOfQuarter(current.getEnd());
	}

	@Override
	public List<OutRow> createOutput(int primaryId, String[] row, PPColumn[] columns, int inputSource, long lineId) throws ParsingException {
		DaysInRangeEntry entries = create(primaryId, row, columns, inputSource, lineId);

		merger.process(entries);

		return retrieveCurrent();
	}

	private List<OutRow> retrieveCurrent() throws ParsingException {
		List<OutRow> result = new ArrayList<>();
		for (PatientEvent patientEvent : emitted) {
			OutRow outRow = toOutRow(patientEvent);
			result.add(outRow);
		}
		emitted.clear();
		return result;
	}

	@Override @JsonIgnore
	public int getWidth() {
		return CONSTANT_COLUMN_OFFSET + identifiers.size();
	}

	@Override
	public ColumnDescription getColumnDescription(int column) {
		switch (column) {
			case 0: return new ColumnDescription("date", MajorTypeId.DATE_RANGE);
			case 1: return new ColumnDescription("dayType", MajorTypeId.STRING);
			case 2: return new ColumnDescription("date_start", MajorTypeId.DATE);
			case 3: return new ColumnDescription("date_end", MajorTypeId.DATE);
			default: return getIdentifierColumnDescription(column - CONSTANT_COLUMN_OFFSET);
		}
	}

	private ColumnDescription getIdentifierColumnDescription(int index) {
		Output identifier = identifiers.get(index);
		return identifier.getColumnDescription();
	}

	@Override
	public List<OutRow> finish() throws ParsingException {
		merger.clearRemaining();
		return retrieveCurrent();
	}

	private OutRow toOutRow(PatientEvent patientEvent) throws ParsingException {

		Object[] result = new Object[getWidth()];
		result[0] = patientEvent.getRange();

		result[1] = patientEvent.getColumns()[1].getType().parse(patientEvent.getType());

		result[2] = CDate.ofLocalDate(patientEvent.getRange().getMin());
		result[3] = CDate.ofLocalDate(patientEvent.getRange().getMax());

		for(int i = 0; i < identifiers.size(); i++) {
			result[i + CONSTANT_COLUMN_OFFSET] = patientEvent.getIdentifier().getValue(i);
		}

		return new OutRow(patientEvent.getPrimaryId(), patientEvent.getColumns(), result);
	}


	private DaysForType getDays(String[] values) {
		return getDaysForType(values, dayType);
	}

	private DaysForType getDaysForType(String[] values, TypeId typeId) {
		String type = typeId.getType();
		int days = getDays(values, typeId.getColumn());
		return new DaysForType(days, type);
	}

	private int getDays(String[] values, int dayId) {
		return Ints.tryParse(values[dayId]);
	}

	private CDateRange getQuarter(String[] values) {
		int year = getYear(values);
		int quarter = getQuarterId(values);
		return QuarterUtils.fromQuarter(year, quarter);
	}

	private int getYear(String[] values) {
		return Integer.parseInt(values[yearColumn]);
	}

	private int getQuarterId(String[] values) {
		return Integer.parseInt(values[quarterColumn]);
	}

	private DaysInRangeEntry create(int primaryId, String[] values, PPColumn[] columns, int inputSource, long lineId) throws ParsingException {
		List<Object> identifierValues = new ArrayList<>();

		for (int idIndex = 0; idIndex < identifiers.size(); idIndex++) {
			Output output = identifiers.get(idIndex);

			List<Object> outputValues = output.createOutput(columns[idIndex + CONSTANT_COLUMN_OFFSET].getType(), values, inputSource, lineId);

			if (outputValues.size() != 1) {
				throw new IllegalStateException("Unsupported identifier format");
			}

			identifierValues.add(outputValues.get(0));
		}

		Identifier identifier = new Identifier(identifierValues);

		DaysForType daysForType = getDays(values);
		CDateRange quarter = getQuarter(values);

		int days = daysForType.getDays();
		String type = daysForType.getType();
		DaysInRange daysInRange = new DaysInRange(quarter, days);

		return new DaysInRangeEntry(primaryId, columns, daysInRange, identifier, type);
	}

	@NoArgsConstructor
	@Setter
	@Getter
	public static class TypeId {

		@NotNull
		private String type;
		@Min(0)
		private int column;
	}

	@Data
	private static class DaysForType {

		private final int days;
		private final String type;
	}
}
