import React from "react";
import T from "i18n-react";

import InputSelect from "../form-components/InputSelect";

import type { SelectedDateColumnT } from "../standard-query-editor/types";

type PropsT = {
  dateColumn: SelectedDateColumnT,
  onSelectDateColumn: (dateColum: string) => void
};

export default ({ dateColumn, onSelectDateColumn }: PropsT) => {
  return (
    <div>
      <InputSelect
        label={T.translate("queryNodeEditor.dateColumn")}
        options={dateColumn.options}
        input={{
          value: dateColumn.value,
          onChange: onSelectDateColumn
        }}
      />
    </div>
  );
};
