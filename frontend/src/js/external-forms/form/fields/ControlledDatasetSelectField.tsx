import type { ComponentProps } from "react";
import type { SelectOptionT } from "../../../api/types";
import { ComboBoxField } from "../../../ui-components/ComboBoxField";
import type { DatasetSelectField as DatasetSelectFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledDatasetSelectField = ({
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
      errorInField
    >
      {({ value, errorMessage }) => (
        <ComboBoxField
          label={field.label[locale] || ""}
          options={availableDatasets}
          tooltip={field.tooltip ? field.tooltip[locale] : undefined}
          value={(value as SelectOptionT | null) ?? null}
          onChange={(value) => setValue(field.name, value, setValueConfig)}
          errorMessage={errorMessage}
        />
      )}
    </ConnectedField>
  );
};
