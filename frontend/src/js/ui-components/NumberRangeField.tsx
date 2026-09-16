import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { CurrencyConfigT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { numberPatternConstraints } from "../common/helpers/numberPattern";
import { Label } from "./Label";
import { NumberField } from "./NumberField";
import { ToggleButton } from "./ToggleButton";
import { ToggleButtonGroup } from "./ToggleButtonGroup";

// pulled up under the mode switch
const inputs = tv({
  base: "-mt-[3px]",
  variants: {
    mode: {
      range: "grid grid-cols-2 gap-[5px]",
      exact: "",
    },
  },
});

interface ValueT {
  min?: number | null;
  max?: number | null;
  exact?: number | null;
}

export type ModeT = "range" | "exact";
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
  disabled: boolean;
  mode: ModeT;
  stepSize?: number;
  placeholder: string;
  onSwitchMode: (mode: ModeT) => void;
  tooltip?: string;
  pattern?: string | null;
  currencyConfig?: CurrencyConfigT;
}

function getMinMaxExact(value: ValueT | null) {
  if (!value) return { min: null, max: null, exact: null };

  return {
    min: exists(value.min) ? value.min : null,
    max: exists(value.max) ? value.max : null,
    exact: exists(value.exact) ? value.exact : null,
  };
}

export const NumberRangeField = ({
  limits,
  stepSize,
  currencyConfig,
  pattern,
  mode,
  moneyRange,
  placeholder,
  disabled,
  label,
  indexPrefix,
  unit,
  tooltip,
  onSwitchMode,
  value,
  onChange,
}: PropsType) => {
  const { t } = useTranslation();
  const val = getMinMaxExact(value);
  const isRangeMode = mode === "range";

  const constraints = numberPatternConstraints(pattern);
  // money is stored in the smallest unit and shown in the major one
  const money = moneyRange && currencyConfig ? currencyConfig : null;
  const factor = money ? 10 ** money.decimalScale : 1;
  const fractionDigits = money
    ? money.decimalScale
    : constraints.maximumFractionDigits;

  const numberProps = {
    // an unset bound is null from the backend; react-aria clamps to Number(null)
    minValue: limits?.min ?? constraints.minValue,
    maxValue: limits?.max ?? undefined,
    step: stepSize,
    formatOptions: exists(fractionDigits)
      ? {
          minimumFractionDigits: money ? fractionDigits : undefined,
          maximumFractionDigits: fractionDigits,
        }
      : undefined,
    unit: money?.unit,
    placeholder,
    isDisabled: disabled,
    labelSize: "sm" as const,
  };

  const toDisplay = (stored: number | null) =>
    exists(stored) ? stored / factor : null;
  const toStored = (shown: number | null) =>
    exists(shown) ? Math.round(shown * factor) : null;

  const onChangeValue = (
    type: "exact" | "max" | "min",
    shown: number | null,
  ) => {
    const nextValue = toStored(shown);

    if (type === "exact") {
      onChange(nextValue === null ? null : { exact: nextValue });
      return;
    }

    // Clearing the one bound that was still set clears the whole range
    const otherBoundIsEmpty =
      !!value && (type === "max" ? value.min === null : value.max === null);
    if (nextValue === null && otherBoundIsEmpty) {
      onChange(null);
      return;
    }

    onChange({
      min: value ? value.min : null,
      max: value ? value.max : null,
      [type]: nextValue,
    });
  };

  return (
    <div>
      <Label
        elementType="span"
        isDisabled={disabled}
        indexPrefix={indexPrefix}
        tooltip={tooltip}
      >
        {label}
        {unit && ` ( ${unit} )`}
      </Label>
      <ToggleButtonGroup
        size="sm"
        selectionMode="single"
        disallowEmptySelection
        selectedKeys={[mode || "range"]}
        onSelectionChange={(keys) => {
          const [key] = keys;
          if (key === "range" || key === "exact") onSwitchMode(key);
        }}
      >
        <ToggleButton id="range">{t("inputRange.range")}</ToggleButton>
        <ToggleButton id="exact">{t("inputRange.exact")}</ToggleButton>
      </ToggleButtonGroup>
      <div className={inputs({ mode })}>
        {isRangeMode ? (
          <>
            <NumberField
              {...numberProps}
              label={t("inputRange.minLabel")}
              value={toDisplay(val.min)}
              onChange={(shown) => onChangeValue("min", shown)}
            />
            <NumberField
              {...numberProps}
              label={t("inputRange.maxLabel")}
              value={toDisplay(val.max)}
              onChange={(shown) => onChangeValue("max", shown)}
            />
          </>
        ) : (
          <NumberField
            {...numberProps}
            label={t("inputRange.exactLabel")}
            value={toDisplay(val.exact)}
            onChange={(shown) => onChangeValue("exact", shown)}
          />
        )}
      </div>
    </div>
  );
};
