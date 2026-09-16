import type { ComponentProps } from "react";
import { CheckboxField } from "../../../ui-components/CheckboxField";
import type { CheckboxField as CheckboxFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledCheckboxField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: CheckboxFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
      noLabel
      errorInField
    >
      {({ value, errorMessage }) => (
        <CheckboxField
          isSelected={value as boolean}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          errorMessage={errorMessage}
        >
          {field.label[locale] || ""}
        </CheckboxField>
      )}
    </ConnectedField>
  );
};
