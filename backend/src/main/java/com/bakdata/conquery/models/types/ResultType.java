package com.bakdata.conquery.models.types;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.bakdata.conquery.models.events.MajorTypeId;
import com.bakdata.conquery.models.query.PrintSettings;
import com.bakdata.conquery.sql.execution.ResultSetProcessor;
import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public sealed interface ResultType permits ResultType.Primitive, ResultType.ListT {

	public static ResultType resolveResultType(MajorTypeId majorTypeId) {
		return switch (majorTypeId) {
			case STRING -> Primitive.STRING;
			case BOOLEAN -> Primitive.BOOLEAN;
			case DATE -> Primitive.DATE;
			case DATE_RANGE -> Primitive.DATE_RANGE;
			case INTEGER -> Primitive.INTEGER;
			case MONEY -> Primitive.MONEY;
			case DECIMAL, REAL -> Primitive.NUMERIC;
		};
	}

	String typeInfo();

	<T> T getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;

	<T> List<T> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException;

	enum Primitive implements ResultType {
		BOOLEAN {
			@Override
			public Boolean getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getBoolean(resultSet, columnIndex);
			}

			@Override
			public List<Boolean> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getBooleanList(resultSet, columnIndex);
			}
		}, INTEGER {
			@Override
			public Integer getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getInteger(resultSet, columnIndex);
			}

			@Override
			public List<Integer> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getIntegerList(resultSet, columnIndex);
			}
		}, NUMERIC {
			@Override
			public Double getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDouble(resultSet, columnIndex);
			}

			@Override
			public List<Double> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDoubleList(resultSet, columnIndex);
			}
		}, DATE {
			@Override
			public Number getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDate(resultSet, columnIndex);
			}

			@Override
			public List<Number> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDateList(resultSet, columnIndex);
			}
		}, DATE_RANGE {
			@Override
			public List<Integer> getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDateRange(resultSet, columnIndex);
			}

			@Override
			public List<List<Integer>> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getDateRangeList(resultSet, columnIndex);
			}
		}, STRING {
			@Override
			public String getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getString(resultSet, columnIndex);
			}

			@Override
			public List<String> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getStringList(resultSet, columnIndex);
			}
		}, MONEY {
			@Override
			public BigDecimal getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getMoney(resultSet, columnIndex);
			}

			@NotNull
			public BigDecimal readIntermediateValue(PrintSettings cfg, Number f) {
				return new BigDecimal(f.longValue()).movePointLeft(cfg.getCurrency().getDefaultFractionDigits());
			}

			@Override
			public List<BigDecimal> getFromResultSetAsList(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
				return resultSetProcessor.getMoneyList(resultSet, columnIndex);
			}
		};

		@Override
		public String toString() {
			return typeInfo();
		}

		@Override
		public String typeInfo() {
			return name();
		}

		public <T> T readIntermediateValue(PrintSettings cfg, Object f) {
			return (T) f;
		}

	}

	@Data
	@EqualsAndHashCode(callSuper = false)
	final class ListT<T> implements ResultType {

		@NonNull
		private final ResultType elementType;

		@JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
		public ListT(@NonNull ResultType elementType) {
			this.elementType = elementType;
		}

		@Override
		public List<T> getFromResultSet(ResultSet resultSet, int columnIndex, ResultSetProcessor resultSetProcessor) throws SQLException {
			return elementType.getFromResultSetAsList(resultSet, columnIndex, resultSetProcessor);
		}

		@Override
		public List<List<T>> getFromResultSetAsList(final ResultSet resultSet, final int columnIndex, final ResultSetProcessor resultSetProcessor) {
			throw new UnsupportedOperationException("Nested lists not supported in SQL mode");
		}

		@Override
		public String toString() {
			return typeInfo();
		}

		@Override
		public String typeInfo() {
			return "LIST[" + elementType.typeInfo() + "]";
		}
	}
}
