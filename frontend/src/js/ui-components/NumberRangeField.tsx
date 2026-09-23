import { useId } from "react";
import { Group } from "react-aria-components";
import { useTranslation } from "react-i18next";

import type { CurrencyConfigT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { Label } from "./Label";
import { NumberField } from "./NumberField";

interface ValueT {
  min?: number | null;
  max?: number | null;
}

interface PropsType {
  moneyRange?: boolean;
  label: string;
  indexPrefix?: number;
  unit?: string;
  value: ValueT | null;
  onChange: (value: ValueT | null) => void;
  limits?: {
    min?: number | null;
    max?: number | null;
  };
  isDisabled?: boolean;
  stepSize?: number;
  tooltip?: string;
  currencyConfig?: CurrencyConfigT;
}

/** Two bounds, each optional: one alone is an open range, equal bounds an exact value. */
export const NumberRangeField = ({
  limits,
  stepSize,
  currencyConfig,
  moneyRange,
  isDisabled,
  label,
  indexPrefix,
  unit,
  tooltip,
  value,
  onChange,
}: PropsType) => {
  const { t } = useTranslation();
  const labelId = useId();
  const min = value?.min ?? null;
  const max = value?.max ?? null;

  // money is stored in the smallest unit and shown in the major one
  const money = moneyRange && currencyConfig ? currencyConfig : null;
  const factor = money ? 10 ** money.decimalScale : 1;

  const numberProps = {
    step: stepSize,
    formatOptions: money
      ? {
          minimumFractionDigits: money.decimalScale,
          maximumFractionDigits: money.decimalScale,
        }
      : undefined,
    unit: money?.unit,
    isDisabled,
  };

  const toDisplay = (stored: number | null) =>
    exists(stored) ? stored / factor : null;
  const toStored = (shown: number | null) =>
    exists(shown) ? Math.round(shown * factor) : null;

  const onChangeBound = (bound: "min" | "max", shown: number | null) => {
    const next = { min, max, [bound]: toStored(shown) };

    onChange(next.min === null && next.max === null ? null : next);
  };

  // an unset limit is null from the backend; react-aria clamps to Number(null)
  const lowest = limits?.min ?? undefined;
  const highest = limits?.max ?? undefined;

  return (
    <Group aria-labelledby={labelId}>
      <Label
        id={labelId}
        elementType="span"
        isDisabled={isDisabled}
        indexPrefix={indexPrefix}
        tooltip={tooltip}
      >
        {label}
        {unit && ` ( ${unit} )`}
      </Label>
      {/* each bound limits the other, so min never exceeds max */}
      <div className="grid grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] items-center gap-[5px]">
        <NumberField
          {...numberProps}
          aria-label={t("inputRange.minLabel")}
          placeholder={t("inputRange.minLabel")}
          minValue={lowest}
          maxValue={toDisplay(max) ?? highest}
          value={toDisplay(min)}
          onChange={(shown) => onChangeBound("min", shown)}
        />
        <span aria-hidden className="text-gray-600">
          –
        </span>
        <NumberField
          {...numberProps}
          aria-label={t("inputRange.maxLabel")}
          placeholder={t("inputRange.maxLabel")}
          minValue={toDisplay(min) ?? lowest}
          maxValue={highest}
          value={toDisplay(max)}
          onChange={(shown) => onChangeBound("max", shown)}
        />
      </div>
    </Group>
  );
};
