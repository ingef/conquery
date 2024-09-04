package com.bakdata.conquery.models.events.stores.specific;

import java.math.BigDecimal;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.jackson.Initializing;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.stores.root.ColumnStore;
import com.bakdata.conquery.models.events.stores.root.IntegerStore;
import com.bakdata.conquery.models.events.stores.root.MoneyStore;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@CPSType(base = ColumnStore.class, id = "MONEY_VARINT")
@Data
@ToString(of = "numberType")
@NoArgsConstructor(onConstructor_ = {@JsonCreator})
@JsonDeserialize(converter = MoneyIntStore.MoneyIntStoreInitializer.class)
public class MoneyIntStore implements MoneyStore, Initializing {

	@JsonIgnore
	@JacksonInject(useInput = OptBoolean.FALSE)
	@NotNull
	@EqualsAndHashCode.Exclude
	private ConqueryConfig config;

	private IntegerStore numberType;

	@JsonProperty(required = false)
	private int decimalShift = Integer.MIN_VALUE;


	public MoneyIntStore(IntegerStore store, int decimalShift){
		this();
		this.numberType = store;
		this.decimalShift = decimalShift;
	}

	@Override
	public int getLines() {
		return numberType.getLines();
	}

	@Override
	public MoneyIntStore createDescription() {
		return new MoneyIntStore(numberType.createDescription(), getDecimalShift());
	}

	@Override
	public MoneyIntStore select(int[] starts, int[] length) {
		return new MoneyIntStore(numberType.select(starts, length), getDecimalShift());
	}

	@Override
	public BigDecimal getMoney(int event) {
		return BigDecimal.valueOf(numberType.getInteger(event)).movePointLeft(decimalShift);
	}

	@Override
	public long estimateEventBits() {
		return numberType.estimateEventBits();
	}

	@Override
	public void setMoney(int event, BigDecimal value) {
		numberType.setInteger(event, value.movePointRight(decimalShift).longValue());
	}

	@Override
	public void setNull(int event) {
		numberType.setNull(event);
	}

	@Override
	public final boolean has(int event) {
		return numberType.has(event);
	}

	public void setParent(Bucket bucket) {
		// not used
	}

	@Override
	public void init() {
		if (decimalShift != Integer.MIN_VALUE){
			return;
		}

		decimalShift = config.getFrontend().getCurrency().getDecimalScale();
	}

	public static class MoneyIntStoreInitializer extends Initializing.Converter<MoneyIntStore> {}

}
