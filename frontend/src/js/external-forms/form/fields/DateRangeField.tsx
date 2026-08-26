import type { ComponentProps } from "react";
import type { DateStringMinMax } from "../../../common/helpers/dateHelper";
import InputDateRange from "../../../ui-components/InputDateRange";
import type { DateRangeField as DateRangeFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const DateRangeField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: DateRangeFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
    >
      {({ ref, ...fieldProps }) => {
        return (
          <InputDateRange
            inline={true}
            label={field.label[locale]}
            tooltip={field.tooltip ? field.tooltip[locale] : undefined}
            value={fieldProps.value as DateStringMinMax}
            onChange={(value) => setValue(field.name, value, setValueConfig)}
          />
        );
      }}
    </ConnectedField>
  );
};
