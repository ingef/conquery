// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import InputSelect from "../form-components/InputSelect";

import type { SelectedDateColumnT } from "../standard-query-node-editor/types";

type PropsT = {
  dateColumn: SelectedDateColumnT,
  onSelectDateColumn: string => void
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
