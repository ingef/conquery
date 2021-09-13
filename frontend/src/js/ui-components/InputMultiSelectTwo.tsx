import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { useState } from "react";
import { FixedSizeList } from "react-window";

import IconButton from "../button/IconButton";
import InfoTooltip from "../tooltip/InfoTooltip";

import Labeled from "./Labeled";

const Container = styled("div")`
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: 4px;
  display: flex;
  align-items: center;
  overflow: hidden;
`;
const Menu = styled("div")`
  border-radius: 4px;
  box-shadow: 0 0 0 1px hsl(0deg 0% 0% / 10%), 0 4px 11px hsl(0deg 0% 0% / 10%);
`;
const OptionContainer = styled("div")`
  padding: 2px;
  border-radius: 4px;
`;
const Input = styled("input")`
  border: 0;
  width: 100%;
  height: 30px;
`;
const ListOption = styled("div")`
  padding: 2px;
`;
const SxLabeled = styled(Labeled)`
  padding: 2px;
`;
const SxIconButton = styled(IconButton)`
  padding: 6px 8px;
`;

interface SelectOption {
  label: string;
  value: string;
}

interface Props {
  label?: string;
  disabled?: boolean;
  options: SelectOption[];
  tooltip?: string;
  indexPrefix?: number;
  input: {
    value: SelectOption[];
    defaultValue?: SelectOption[];
    onChange: (value: SelectOption[]) => void;
  };
}

const Option = ({
  option,
  onRemoveClick,
}: {
  option: SelectOption;
  onRemoveClick: () => void;
}) => {
  return (
    <OptionContainer>
      {option.label}
      <IconButton icon="times" onClick={onRemoveClick} />
    </OptionContainer>
  );
};

const InputMultiSelectTwo = ({
  options,
  input,
  disabled,
  label,
  tooltip,
  indexPrefix,
}: Props) => {
  const [inputValue, setInputValue] = useState("");
  const {
    getSelectedItemProps,
    getDropdownProps,
    addSelectedItem,
    removeSelectedItem,
    selectedItems,
  } = useMultipleSelection<SelectOption>({
    initialSelectedItems: input.defaultValue,
  });

  const getFilteredOptions = (opts: SelectOption[]) =>
    opts.filter(
      (option) =>
        selectedItems.indexOf(option) < 0 &&
        option.label.toLowerCase().startsWith(inputValue.toLowerCase()),
    );
  const filteredOptions = getFilteredOptions(options);

  const {
    isOpen,
    getToggleButtonProps,
    getLabelProps,
    getMenuProps,
    getInputProps,
    getComboboxProps,
    getItemProps,
    selectItem,
  } = useCombobox({
    inputValue,
    items: filteredOptions,
    onStateChange: ({ inputValue, type, selectedItem }) => {
      switch (type) {
        case useCombobox.stateChangeTypes.InputChange:
          setInputValue(inputValue);

          break;
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.ItemClick:
        case useCombobox.stateChangeTypes.InputBlur:
          if (selectedItem) {
            setInputValue("");
            addSelectedItem(selectedItem);
            selectItem(null);
          }

          break;
        default:
          break;
      }
    },
  });

  return (
    <SxLabeled
      label={
        <>
          {label}
          {tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
      indexPrefix={indexPrefix}
      {...getLabelProps()}
    >
      <Container {...getComboboxProps()}>
        {selectedItems.map((option, index) => (
          <Option
            option={option}
            onRemoveClick={() => removeSelectedItem(option)}
            {...getSelectedItemProps({ selectedItem: option, index })}
          />
        ))}
        <Input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          {...getInputProps(getDropdownProps({ preventKeyAction: isOpen }))}
        />
        <SxIconButton icon="caret-down" {...getToggleButtonProps()} />
      </Container>
      <Menu {...getMenuProps()}>
        {isOpen && (
          <FixedSizeList
            height={300}
            itemCount={filteredOptions.length}
            itemSize={30}
            width="100%"
          >
            {({ index, style }) => (
              <ListOption
                {...style}
                {...getItemProps({ index, item: filteredOptions[index] })}
              ></ListOption>
            )}
          </FixedSizeList>
        )}
      </Menu>
    </SxLabeled>
  );
};

export default InputMultiSelectTwo;
