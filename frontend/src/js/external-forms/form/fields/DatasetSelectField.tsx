import { ComponentProps } from "react";
import { SelectOptionT } from "../../../api/types";
import InputSelect from "../../../ui-components/InputSelect/InputSelect";
import { DatasetSelectField as DatasetSelectFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import Field from "../Field";

export const DatasetSelectField = ({
  field,
  datasetId,
  commonProps: { control, locale, setValue, availableDatasets },
}: {
  field: DatasetSelectFieldT;
  datasetId: string | null;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={
        availableDatasets.length > 0
          ? availableDatasets.find((opt) => opt.value === datasetId) ||
            availableDatasets[0]
          : null
      }
    >
      {({ ref, ...fieldProps }) => {
        return (
          <InputSelect
            label={field.label[locale]}
            options={availableDatasets}
            tooltip={field.tooltip ? field.tooltip[locale] : undefined}
            value={fieldProps.value as SelectOptionT | null}
            onChange={(value) => setValue(field.name, value, setValueConfig)}
          />
        );
      }}
    </ConnectedField>
  );
};
