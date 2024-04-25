import { ComponentProps } from "react";
import InputPlain from "../../../ui-components/InputPlain/InputPlain";
import type { StringField as StringFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const StringField = ({
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
    >
      {({ ref, ...fieldProps }) => (
        <InputPlain
          ref={ref}
          inputType="text"
          label={field.label[locale] || ""}
          placeholder={(field.placeholder && field.placeholder[locale]) || ""}
          fullWidth={field.style ? field.style.fullWidth : false}
          value={fieldProps.value as string}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
        />
      )}
    </ConnectedField>
  );
};
