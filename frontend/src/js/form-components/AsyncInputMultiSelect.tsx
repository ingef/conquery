import React from "react";
import type { WrappedFieldProps } from "redux-form";

import type { SelectOptionsT } from "../api/types";
import InputMultiSelect from "./InputMultiSelect";

interface PropsType extends WrappedFieldProps {
  label: string;
  isLoading: boolean;
  options: SelectOptionsT;
  disabled?: boolean | null;
  startLoadingThreshold: number;
  tooltip?: string;
  onLoad: Function;
  onDropFile: Function;
  allowDropFile?: boolean | null;
}

const AsyncInputMultiSelect = ({
  startLoadingThreshold,
  onLoad,
  ...props
}: PropsType) => (
  <InputMultiSelect
    {...props}
    onInputChange={(value) => {
      if (value.length >= startLoadingThreshold) onLoad(value);

      return value;
    }}
  />
);

export default AsyncInputMultiSelect;
