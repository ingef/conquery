import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { useState, useMemo } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import InfoTooltip from "../../tooltip/InfoTooltip";
import InputMultiSelectDropzone from "../InputMultiSelectDropzone";
import Labeled from "../Labeled";

import EmptyPlaceholder from "./EmptyPlaceholder";
import ListOption from "./ListOption";
import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";
import { useResolvableSelect } from "./useResolvableSelect";

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

const SelectContainer = styled("div")`
  width: 100%;
  position: relative;
`;

const ItemsInputContainer = styled("div")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 3px;
  width: 100%;
`;

const Menu = styled("div")`
  position: absolute;
  width: 100%;
  border-radius: 4px;
  box-shadow: 0 0 0 1px hsl(0deg 0% 0% / 10%), 0 4px 11px hsl(0deg 0% 0% / 10%);
  background-color: ${({ theme }) => theme.col.bg};
  z-index: 2;
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

const SxInputMultiSelectDropzone = styled(InputMultiSelectDropzone)`
  display: block;
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
  onResolve?: (csvFileLines: string[]) => void; // The assumption is that this will somehow update `options`
}

const InputMultiSelectTwo = ({
  options,
  input,
  label,
  tooltip,
  indexPrefix,
  creatable,
  onResolve,
}: Props) => {
  const { onDropFile } = useResolvableSelect({
    defaultValue: input.defaultValue,
    onResolve,
  });
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
    const creatableOption =
      creatable && inputValue.length > 0
        ? [
            {
              label: `${t("common.create")}: "${inputValue}"`,
              value: inputValue,
            },
          ]
        : [];

    const regularOptions = options.filter(
      (option) =>
        selectedItems.indexOf(option) < 0 &&
        (option.label.toLowerCase().includes(inputValue.toLowerCase()) ||
          String(option.value)
            .toLowerCase()
            .includes(inputValue.toLowerCase())),
    );

    return [...creatableOption, ...regularOptions];
  }, [options, selectedItems, inputValue, creatable, t]);

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
    setHighlightedIndex,
    reset: resetComboboxState,
  } = useCombobox({
    inputValue,
    items: filteredOptions,
    stateReducer: (state, { type, changes }) => {
      // This modifies the action payload itself
      // in that way
      // - the default behavior may be adjusted
      // - including the `onStateChange` reactions that diverge from default behavior (see below)
      switch (type) {
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.InputBlur:
        case useCombobox.stateChangeTypes.ItemClick:
          const stayAlmostAtTheSamePositionIndex =
            state.highlightedIndex === filteredOptions.length - 1
              ? state.highlightedIndex - 1
              : state.highlightedIndex;

          const hasChosenCreatableItem =
            creatable && state.highlightedIndex === 0 && inputValue.length > 0;

          // The item that will be "chosen"
          const selectedItem = hasChosenCreatableItem
            ? { value: inputValue, label: inputValue }
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
      // This only modifies the behavior of some of the actions, after the state has been changed
      switch (action.type) {
        case useCombobox.stateChangeTypes.InputChange:
          if (action.highlightedIndex !== 0) {
            setHighlightedIndex(0);
          }
          break;
        case useCombobox.stateChangeTypes.InputKeyDownEscape:
          if (action.isOpen) {
            // Sometimes closing the menu on esc didn't work, this fixes it
            toggleMenu();
          }
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

  const { ref: menuPropsRef, ...menuProps } = getMenuProps();
  const inputProps = getInputProps(getDropdownProps());
  const { ref: comboboxRef, ...comboboxProps } = getComboboxProps();

  const Select = (
    <SelectContainer>
      <Control
        {...comboboxProps}
        ref={(instance) => {
          comboboxRef(instance);
        }}
      >
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
              onResolve
                ? t("inputMultiSelect.dndPlaceholder")
                : t("inputSelect.placeholder")
            }
            onClick={(e) => {
              if (inputProps.onClick) {
                inputProps.onClick(e);
              }
              toggleMenu();
            }}
            onChange={(e) => {
              if (inputProps.onChange) {
                inputProps.onChange(e);
              }
              setInputValue(e.target.value);
            }}
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
      {isOpen ? (
        <Menu
          {...menuProps}
          ref={(instance) => {
            menuPropsRef(instance);
          }}
        >
          <MenuActionBar
            optionsCount={filteredOptions.length}
            onInsertAllClick={() => {
              setSelectedItems(filteredOptions);
            }}
          />
          <List>
            {!creatable && filteredOptions.length === 0 && <EmptyPlaceholder />}
            {filteredOptions.map((option, index) => {
              return (
                <ListOption
                  key={`${option.value}`}
                  active={highlightedIndex === index}
                  {...getItemProps({ index, item: filteredOptions[index] })}
                >
                  {option.label}
                </ListOption>
              );
            })}
          </List>
        </Menu>
      ) : (
        <span ref={menuPropsRef} /> // To avoid a warning / error by downshift that ref is not applied
      )}
    </SelectContainer>
  );

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
      {!onResolve && Select}
      {onResolve && onDropFile && (
        <SxInputMultiSelectDropzone onDropFile={onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </SxLabeled>
  );
};

export default InputMultiSelectTwo;
