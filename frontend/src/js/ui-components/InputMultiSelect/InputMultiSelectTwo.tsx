import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { useState, useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Labeled from "../Labeled";

import EmptyPlaceholder from "./EmptyPlaceholder";
import ListOption from "./ListOption";
import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";

const Control = styled("div")`
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: 4px;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 3px 3px 3px 8px;
  background-color: white;

  &:focus {
    outline: 1px solid black;
  }
`;
const ItemsInputContainer = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  width: 100%;
`;

const Menu = styled("div")`
  border-radius: 4px;
  box-shadow: 0 0 0 1px hsl(0deg 0% 0% / 10%), 0 4px 11px hsl(0deg 0% 0% / 10%);
`;

const List = styled("div")`
  padding: 3px;
  max-height: 300px;
  overflow-y: auto;
  --webkit-overflow-scrolling: touch;
`;

const Input = styled("input")`
  border: 0;
  height: 24px;
  outline: none;
  flex-grow: 1;
  flex-basis: 30px;
`;
const SxLabeled = styled(Labeled)`
  padding: 2px;
`;
const DropdownToggleButton = styled(IconButton)`
  padding: 5px 6px;
`;
const ResetButton = styled(IconButton)`
  padding: 5px 8px;
`;
const VerticalSeparator = styled("div")`
  width: 1px;
  margin: 3px 0;
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  align-self: stretch;
  flex-shrink: 0;
`;

interface Props {
  label?: string;
  disabled?: boolean;
  options: SelectOptionT[];
  tooltip?: string;
  indexPrefix?: number;
  creatable?: boolean;
  input: {
    value: SelectOptionT[];
    defaultValue?: SelectOptionT[];
    onChange: (value: SelectOptionT[]) => void;
  };
  onDropFile?: (file: File) => void;
}

const InputMultiSelectTwo = ({
  options,
  input,
  label,
  tooltip,
  indexPrefix,
  creatable,
  onDropFile,
}: Props) => {
  const [inputValue, setInputValue] = useState("");
  const { t } = useTranslation();
  const {
    getSelectedItemProps,
    getDropdownProps,
    addSelectedItem,
    removeSelectedItem,
    selectedItems,
    reset: resetMultiSelectState,
    setSelectedItems,
    activeIndex,
  } = useMultipleSelection<SelectOptionT>({
    initialSelectedItems: input.defaultValue,
    onSelectedItemsChange: (changes) => {
      if (changes.selectedItems) {
        input.onChange(changes.selectedItems);
      }
    },
  });

  const filteredOptions = useMemo(() => {
    return options.filter(
      (option) =>
        selectedItems.indexOf(option) < 0 &&
        (option.label.toLowerCase().includes(inputValue.toLowerCase()) ||
          String(option.value)
            .toLowerCase()
            .includes(inputValue.toLowerCase())),
    );
  }, [options, selectedItems, inputValue]);

  const {
    isOpen,
    toggleMenu,
    getToggleButtonProps,
    getLabelProps,
    getMenuProps,
    getInputProps,
    getComboboxProps,
    getItemProps,
    highlightedIndex,
    reset: resetComboboxState,
  } = useCombobox({
    inputValue,
    items: filteredOptions,
    stateReducer: (state, { type, changes }) => {
      switch (type) {
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.ItemClick:
          const stayAlmostAtTheSamePositionIndex =
            state.highlightedIndex === filteredOptions.length - 1
              ? state.highlightedIndex - 1
              : state.highlightedIndex;

          const selectedItem =
            creatable && state.highlightedIndex === 0
              ? { value: inputValue, label: inputValue }
              : creatable
              ? filteredOptions[state.highlightedIndex - 1]
              : changes.selectedItem;

          return {
            ...changes,
            selectedItem,
            isOpen: true,
            highlightedIndex: stayAlmostAtTheSamePositionIndex,
          };
        default:
          return changes;
      }
    },
    onStateChange: (action) => {
      switch (action.type) {
        case useCombobox.stateChangeTypes.InputChange:
          setInputValue(action.inputValue ? action.inputValue : "");
          break;
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.InputBlur:
        case useCombobox.stateChangeTypes.ItemClick:
          if (action.selectedItem) {
            addSelectedItem(action.selectedItem);

            const wasNewItemCreated =
              creatable &&
              action.selectedItem.value === inputValue &&
              action.selectedItem.label === inputValue;

            if (wasNewItemCreated) {
              setInputValue("");
            }
          }
          break;
        default:
          break;
      }
    },
  });

  const menuProps = getMenuProps();
  const inputProps = getInputProps(getDropdownProps());

  return (
    <SxLabeled
      {...getLabelProps({})}
      htmlFor="" // Important to override getLabelProps with this to avoid click events everywhere
      label={
        <>
          {label}
          {tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
      indexPrefix={indexPrefix}
    >
      <Control {...getComboboxProps()}>
        <ItemsInputContainer>
          {selectedItems.map((option, index) => {
            const selectedItemProps = getSelectedItemProps({
              selectedItem: option,
              index,
            });

            return (
              <SelectedItem
                key={`${option.value}${index}`}
                option={option}
                active={index === activeIndex}
                {...selectedItemProps}
                onRemoveClick={() => removeSelectedItem(option)}
              />
            );
          })}
          <Input
            type="text"
            value={inputValue}
            {...inputProps}
            placeholder={
              onDropFile
                ? t("inputMultiSelect.dndPlaceholder")
                : t("inputSelect.placeholder")
            }
            onClick={(e) => {
              if (inputProps.onClick) {
                inputProps.onClick(e);
              }
              toggleMenu();
            }}
            onChange={(e) => setInputValue(e.target.value)}
          />
        </ItemsInputContainer>
        {(inputValue.length > 0 || selectedItems.length > 0) && (
          <ResetButton
            icon="times"
            onClick={() => {
              setInputValue("");
              resetMultiSelectState();
              resetComboboxState();
            }}
          />
        )}
        <VerticalSeparator />
        <DropdownToggleButton icon="chevron-down" {...getToggleButtonProps()} />
      </Control>
      <Menu {...menuProps}>
        {isOpen && (
          <>
            <MenuActionBar
              optionsCount={filteredOptions.length}
              onInsertAllClick={() => {
                setSelectedItems(filteredOptions);
              }}
            />
            <List>
              {!creatable && filteredOptions.length === 0 && (
                <EmptyPlaceholder />
              )}
              {creatable && inputValue.length > 0 && (
                <ListOption
                  active={highlightedIndex === 0}
                  {...getItemProps({
                    index: 0,
                    item: { value: inputValue, label: inputValue },
                  })}
                >
                  {t("common.create") + `: "${inputValue}"`}
                </ListOption>
              )}
              {filteredOptions.map((option, index) => {
                const correctedIndex = creatable ? index + 1 : index;
                return (
                  <ListOption
                    key={`${option.value}`}
                    active={highlightedIndex === correctedIndex}
                    {...getItemProps({
                      index: correctedIndex,
                      item: filteredOptions[index],
                    })}
                  >
                    {option.label}
                  </ListOption>
                );
              })}
            </List>
          </>
        )}
      </Menu>
    </SxLabeled>
  );
};

export default InputMultiSelectTwo;
