import { ComponentProps } from "react";
import { SelectOptionT } from "../../../api/types";
import InputSelect from "../../../ui-components/InputSelect/InputSelect";
import { SelectField as SelectFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const SelectField = ({
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
    >
      {({ ref, ...fieldProps }) => (
        <InputSelect
          label={field.label[locale]}
          options={field.options.map((option) => ({
            label: option.label[locale] || "",
            value: option.value,
          }))}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          value={fieldProps.value as SelectOptionT | null}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
        />
      )}
    </ConnectedField>
  );
};
