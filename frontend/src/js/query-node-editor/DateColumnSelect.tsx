import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import InputSelect from "../form-components/InputSelect";
import type { SelectedDateColumnT } from "../standard-query-editor/types";

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
        input={{
          value: dateColumn.value,
          onChange: onSelectDateColumn,
        }}
      />
    </div>
  );
};

export default DateColumnSelect;
