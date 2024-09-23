import { ComponentProps } from "react";
import InputPlain from "../../../ui-components/InputPlain/InputPlain";
import { NumberField as NumberFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const NumberField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: NumberFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
    >
      {({ ref, ...fieldProps }) => (
        <InputPlain
          ref={ref}
          inputType="number"
          label={field.label[locale] || ""}
          placeholder={(field.placeholder && field.placeholder[locale]) || ""}
          value={fieldProps.value as number | null}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          inputProps={{
            step: field.step || "1",
            pattern: field.pattern,
            min: field.min,
            max: field.max,
          }}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
        />
      )}
    </ConnectedField>
  );
};
