package com.bakdata.conquery.models.query.resultinfo.printers;

import java.math.BigDecimal;
import java.util.Objects;

import com.bakdata.conquery.models.query.PrintSettings;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

public class JsonResultPrinters extends PrinterFactory {

	private final JavaResultPrinters delegate = new JavaResultPrinters();

	@Override
	public Printer getListPrinter(Printer elementPrinter, PrintSettings printSettings) {
		return delegate.getListPrinter(elementPrinter, printSettings);
	}

	public record BooleanPrinter() implements Printer {

		@Override
		public Object apply(Object value) {
			return BooleanNode.valueOf(((Boolean) value));
		}
	}

	@Override
	public Printer getBooleanPrinter(PrintSettings printSettings) {
		return new BooleanPrinter();
	}

	public record IntegerPrinter() implements Printer {

		@Override
		public Object apply(Object value) {
			return IntNode.valueOf(((Integer) value));
		}
	}

	@Override
	public Printer getIntegerPrinter(PrintSettings printSettings) {
		return new IntegerPrinter();
	}

	public record NumericPrinter() implements Printer {

		@Override
		public Object apply(Object value) {
			return DecimalNode.valueOf(((BigDecimal) value));
		}
	}

	@Override
	public Printer getNumericPrinter(PrintSettings printSettings) {
		return new NumericPrinter();
	}

	public record ToStringPrinter(Printer delegate) implements Printer {

		@Override
		public Object apply(Object value) {
			return new TextNode(Objects.toString(delegate.apply(value)));
		}
	}

	@Override
	public Printer getDatePrinter(PrintSettings printSettings) {
		//TODO compare with current impl
		return new ToStringPrinter(delegate.getDatePrinter(printSettings));
	}

	@Override
	public Printer getDateRangePrinter(PrintSettings printSettings) {
		return new ToStringPrinter(delegate.getDateRangePrinter(printSettings));
	}

	public record StringPrinter() implements Printer {

		@Override
		public Object apply(Object value) {
			return new TextNode((String) value);
		}
	}
	@Override
	public Printer getStringPrinter(PrintSettings printSettings) {
		return new StringPrinter();
	}

	@Override
	public Printer getMoneyPrinter(PrintSettings printSettings) {
		return delegate.getMoneyPrinter(printSettings);
	}
}
