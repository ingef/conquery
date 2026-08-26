import type { ComponentProps } from "react";
import InputCheckbox from "../../../ui-components/InputCheckbox";
import type { CheckboxField as CheckboxFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const CheckboxField = ({
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
    >
      {({ ref, ...fieldProps }) => (
        <InputCheckbox
          value={fieldProps.value as boolean}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          label={field.label[locale] || ""}
          infoTooltip={field.tooltip ? field.tooltip[locale] : undefined}
        />
      )}
    </ConnectedField>
  );
};
