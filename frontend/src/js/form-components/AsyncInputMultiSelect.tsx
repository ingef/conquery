import React from "react";
import InputMultiSelect, { InputMultiSelectProps } from "./InputMultiSelect";

interface PropsType extends InputMultiSelectProps {
  startLoadingThreshold: number;
  onLoad: Function;
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
