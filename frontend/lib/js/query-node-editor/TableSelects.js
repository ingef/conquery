// @flow

import React from "react";
import styled from "@emotion/styled";

import type { SelectedSelectorType } from "./types";

import InputMultiSelect from "../form-components/InputMultiSelect";

type PropsType = {
  selects: SelectedSelectorType[],
  onSelectTableSelects: () => void,
  excludeTable: boolean
};

const TableSelects = ({
  selects,
  onSelectTableSelects,
  excludeTable
}: PropsType) => {
  if (!selects) return null;

  return (
    <div>
      <InputMultiSelect
        input={{
          onChange: onSelectTableSelects,
          value: selects
            .filter(({ selected }) => !!selected)
            .map(({ id, label }) => ({ value: id, label: label }))
        }}
        options={selects.map(select => ({
          value: select.id,
          label: select.label
        }))}
        disabled={excludeTable}
      />
    </div>
  );
};

export default TableSelects;
