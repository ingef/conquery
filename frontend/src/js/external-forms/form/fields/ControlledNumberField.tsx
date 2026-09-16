import type { ComponentProps } from "react";
import { exists } from "../../../common/helpers/exists";
import { numberPatternConstraints } from "../../../common/helpers/numberPattern";
import { NumberField } from "../../../ui-components/NumberField";
import type { NumberField as NumberFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledNumberField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: NumberFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const constraints = numberPatternConstraints(field.pattern);

  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
      errorInField
    >
      {({ ref, value, errorMessage }) => (
        <NumberField
          inputRef={ref}
          label={field.label[locale] || ""}
          placeholder={field.placeholder?.[locale] || ""}
          value={(value as number | null) ?? null}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          step={field.step ? Number(field.step) : undefined}
          minValue={field.min ?? constraints.minValue}
          maxValue={field.max}
          formatOptions={
            exists(constraints.maximumFractionDigits)
              ? { maximumFractionDigits: constraints.maximumFractionDigits }
              : undefined
          }
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          errorMessage={errorMessage}
        />
      )}
    </ConnectedField>
  );
};
