import type { ComponentProps } from "react";
import { useTranslation } from "react-i18next";

import {
  formatDateFromState,
  parseDate,
  parseDateToState,
} from "../../../common/helpers/dateHelper";
import { exists } from "../../../common/helpers/exists";
import { DateField } from "../../../ui-components/DateField/DateField";
import type { DateField as DateFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const ControlledDateField = ({
  field,
  defaultValue,
  commonProps: { control, locale, setValue },
}: {
  field: DateFieldT;
  defaultValue: unknown;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const { t } = useTranslation();
  const displayDateFormat = t("inputDateRange.dateFormat");

  const onChange = (val: string) => {
    const date = parseDate(val, displayDateFormat);

    // Keep what was typed until it parses, so the user can see the error
    setValue(field.name, date ? parseDateToState(date) : val, setValueConfig);
  };

  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
      errorInField
    >
      {({ value, errorMessage }) => {
        const stateValue = (value as string | null) ?? "";
        const displayValue = formatDateFromState(stateValue, displayDateFormat);
        const isValid = exists(parseDate(displayValue, displayDateFormat));

        return (
          // content-sized, like each date input of a range
          <div className="w-fit">
            <DateField
              label={field.label[locale] || ""}
              tooltip={field.tooltip ? field.tooltip[locale] : undefined}
              value={displayValue}
              dateFormat={displayDateFormat}
              errorMessage={
                displayValue.length !== 0 && !isValid
                  ? t("common.dateInvalid")
                  : errorMessage
              }
              placeholder={displayDateFormat.toUpperCase()}
              onChange={onChange}
              onBlur={(e) => {
                if (!parseDate(e.target.value, displayDateFormat)) {
                  setValue(field.name, null, setValueConfig);
                }
              }}
            />
          </div>
        );
      }}
    </ConnectedField>
  );
};
