import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import type { ComponentProps } from "react";
import { useTranslation } from "react-i18next";

import {
  formatDateFromState,
  parseDate,
  parseDateToState,
} from "../../../common/helpers/dateHelper";
import { exists } from "../../../common/helpers/exists";
import { Icon } from "../../../ui-components/Icon";
import InfoTooltip from "../../../ui-components/InfoTooltip";
import InputDate from "../../../ui-components/InputDate/InputDate";
import Label from "../../../ui-components/Label";
import type { DateField as DateFieldT } from "../../config-types";
import { ConnectedField, setValueConfig } from "../ConnectedField";
import type Field from "../Field";

export const DateField = ({
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

  const onChange = (raw: string | number | null) => {
    const val = raw === null ? "" : String(raw);
    const date = parseDate(val, displayDateFormat);

    // Keep what was typed until it parses, so the user can see the error
    setValue(field.name, date ? parseDateToState(date) : val, setValueConfig);
  };

  return (
    <ConnectedField
      formField={field}
      control={control}
      defaultValue={defaultValue}
    >
      {({ value }) => {
        const stateValue = (value as string | null) ?? "";
        const displayValue = formatDateFromState(stateValue, displayDateFormat);
        const isValid = exists(parseDate(displayValue, displayDateFormat));

        return (
          <div>
            <Label>
              <Icon icon={faCalendar} className="mr-[10px] text-gray-500" />
              {field.label[locale]}
              {field.tooltip && <InfoTooltip text={field.tooltip[locale]} />}
            </Label>
            <InputDate
              // Content-sized, like each single date input inside InputDateRange
              className="w-fit"
              value={displayValue}
              dateFormat={displayDateFormat}
              valid={isValid}
              invalid={displayValue.length !== 0 && !isValid}
              invalidText={t("common.dateInvalid")}
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
