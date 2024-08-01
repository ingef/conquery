import { ComponentProps } from "react";
import { DragItemQuery } from "../../../standard-query-editor/types";
import { ResultGroupField as ResultGroupFieldT } from "../../config-types";
import FormQueryDropzone from "../../form-query-dropzone/FormQueryDropzone";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const ResultGroupField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: ResultGroupFieldT;
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
        <FormQueryDropzone
          label={field.label[locale] || ""}
          dropzoneText={field.dropzoneLabel[locale] || ""}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          value={fieldProps.value as DragItemQuery}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
        />
      )}
    </ConnectedField>
  );
};
