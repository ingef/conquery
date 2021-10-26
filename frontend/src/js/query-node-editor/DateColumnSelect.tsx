import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { SelectedDateColumnT } from "../standard-query-editor/types";
import InputSelect from "../ui-components/InputSelect/InputSelect";

interface PropsT {
  dateColumn: SelectedDateColumnT;
  onSelectDateColumn: (dateColumn: string | null) => void;
}

const DateColumnSelect: FC<PropsT> = ({ dateColumn, onSelectDateColumn }) => {
  const { t } = useTranslation();

  return (
    <div>
      <InputSelect
        label={t("queryNodeEditor.dateColumn")}
        options={dateColumn.options}
        value={
          dateColumn.options.find((op) => op.value === dateColumn.value) || null
        }
        onChange={(value) => {
          if (value) onSelectDateColumn(value.value as string);
        }}
      />
    </div>
  );
};

export default DateColumnSelect;
