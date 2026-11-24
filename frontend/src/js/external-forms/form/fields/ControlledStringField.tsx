import type { ComponentProps } from "react";
import { TextField } from "../../../ui-components/TextField";
import type { StringField as StringFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledStringField = ({
  field,
  defaultValue,
  commonProps: { locale, control, setValue },
}: {
  field: StringFieldT;
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
      {({ ref, value, errorMessage }) => (
        <TextField
          inputRef={ref}
          label={field.label[locale] || ""}
          placeholder={field.placeholder?.[locale] || ""}
          value={(value as string | null) ?? ""}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          errorMessage={errorMessage}
        />
      )}
    </ConnectedField>
  );
};
