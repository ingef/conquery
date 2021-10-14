import InputMultiSelect, { InputMultiSelectProps } from "./InputMultiSelect";

interface PropsType extends InputMultiSelectProps {
  startLoadingThreshold?: number;
  onLoad: (prefix: string) => void;
}

const AsyncInputMultiSelect = ({
  startLoadingThreshold = 2,
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
