import type { ComponentProps } from "react";
import type { SelectOptionT } from "../../../api/types";
import { ComboBoxField } from "../../../ui-components/ComboBoxField";
import type { SelectField as SelectFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledSelectField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: SelectFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
      errorInField
    >
      {({ value, errorMessage }) => (
        <ComboBoxField
          label={field.label[locale] || ""}
          options={field.options.map((option) => ({
            label: option.label[locale] || "",
            value: option.value,
          }))}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          value={(value as SelectOptionT | null) ?? null}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          errorMessage={errorMessage}
        />
      )}
    </ConnectedField>
  );
};
