// @flow

import React from "react";
import type { FieldPropsType } from "redux-form";

import type { SelectOptionsT } from "../api/types";
import InputMultiSelect from "./InputMultiSelect";

type PropsType = FieldPropsType & {
  label: string,
  isLoading: boolean,
  options: SelectOptionsT,
  disabled?: ?boolean,
  startLoadingThreshold: number,
  tooltip?: string,
  onLoad: Function,
  onDropFile: Function,
  allowDropFile?: ?boolean
};

const AsyncInputMultiSelect = ({
  startLoadingThreshold,
  onLoad,
  ...props
}: PropsType) => (
  <InputMultiSelect
    {...props}
    onInputChange={value => {
      if (value.length >= startLoadingThreshold) onLoad(value);

      return value;
    }}
  />
);

export default AsyncInputMultiSelect;
