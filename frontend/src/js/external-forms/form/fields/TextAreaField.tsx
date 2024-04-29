import { ComponentProps } from "react";
import { InputTextarea } from "../../../ui-components/InputTextarea/InputTextarea";
import { TextareaField } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const TextAreaField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: TextareaField;
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
        <InputTextarea
          ref={ref}
          label={field.label[locale] || ""}
          placeholder={(field.placeholder && field.placeholder[locale]) || ""}
          rows={field.style?.rows ?? 4}
          value={fieldProps.value as string}
          onChange={(value) => {
            setValue(field.name, value, setValueConfig);
          }}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
        />
      )}
    </ConnectedField>
  );
};
