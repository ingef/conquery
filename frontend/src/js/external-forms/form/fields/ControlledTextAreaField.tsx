import type { ComponentProps } from "react";
import { TextAreaField } from "../../../ui-components/TextAreaField";
import type { TextareaField } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledTextAreaField = ({
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
      errorInField
    >
      {({ ref, value, errorMessage }) => (
        <TextAreaField
          inputRef={ref}
          label={field.label[locale] || ""}
          placeholder={field.placeholder?.[locale] || ""}
          rows={field.style?.rows ?? 4}
          value={(value as string | null) ?? ""}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          errorMessage={errorMessage}
        />
      )}
    </ConnectedField>
  );
};
