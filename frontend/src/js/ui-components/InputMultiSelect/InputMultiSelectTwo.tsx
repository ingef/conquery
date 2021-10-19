import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { useState } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import InfoTooltip from "../../tooltip/InfoTooltip";
import InputMultiSelectDropzone from "../InputMultiSelectDropzone";
import {
  Control,
  DropdownToggleButton,
  Input,
  ItemsInputContainer,
  List,
  Menu,
  ResetButton,
  SelectContainer,
  SxLabeled,
  VerticalSeparator,
} from "../InputSelect/InputSelectComponents";
import EmptyPlaceholder from "../SelectEmptyPlaceholder";
import SelectListOption from "../SelectListOption";
import TooManyValues from "../TooManyValues";

import LoadMoreSentinel from "./LoadMoreSentinel";
import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";
import { useCloseOnClickOutside } from "./useCloseOnClickOutside";
import { useFilteredOptions } from "./useFilteredOptions";
import { useResolvableSelect } from "./useResolvableSelect";
import { useSyncWithValueFromAbove } from "./useSyncWithValueFromAbove";

const MAX_VALUES_LIMIT = 200;

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
  value: SelectOptionT[];
  defaultValue?: SelectOptionT[];
  onChange: (value: SelectOptionT[]) => void;
  loading?: boolean;
  onResolve?: (csvFileLines: string[]) => void; // The assumption is that this will somehow update `options`
  onLoadMore?: (inputValue: string) => void;
}

const InputMultiSelectTwo = ({
  options,
  label,
  tooltip,
  indexPrefix,
  creatable,
  disabled,
  value,
  defaultValue,
  onChange,
  loading,
  onResolve,
  onLoadMore,
}: Props) => {
  const { onDropFile } = useResolvableSelect({
    defaultValue,
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
    initialSelectedItems: defaultValue,
    onSelectedItemsChange: (changes) => {
      if (changes.selectedItems) {
        onChange(changes.selectedItems);
      }
    },
  });

  const filteredOptions = useFilteredOptions({
    options,
    selectedItems,
    inputValue,
    creatable,
  });

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
          if (changes.selectedItem?.disabled) {
            return state;
          }

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
  const labelProps = getLabelProps({});

  const clickOutsideRef = useCloseOnClickOutside({ isOpen, toggleMenu });

  useSyncWithValueFromAbove({
    inputValueFromAbove: value,
    selectedItems,
    setSelectedItems,
  });

  const Select = (
    <SelectContainer>
      <Control
        {...comboboxProps}
        disabled={disabled}
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
                disabled={disabled}
                {...selectedItemProps}
                onRemoveClick={() => removeSelectedItem(option)}
              />
            );
          })}
          <Input
            type="text"
            value={inputValue}
            {...inputProps}
            disabled={disabled}
            placeholder={
              selectedItems.length > 0
                ? null
                : onResolve
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
            disabled={disabled}
            onClick={() => {
              setInputValue("");
              resetMultiSelectState();
              resetComboboxState();
            }}
          />
        )}
        <VerticalSeparator />
        <DropdownToggleButton
          disabled={disabled}
          icon="chevron-down"
          {...getToggleButtonProps()}
        />
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
              const { ref: itemPropsRef, ...itemProps } = getItemProps({
                index,
                item: filteredOptions[index],
              });

              return (
                <SelectListOption
                  key={`${option.value}`}
                  active={highlightedIndex === index}
                  disabled={option.disabled}
                  {...itemProps}
                  ref={itemPropsRef}
                >
                  {option.label}
                </SelectListOption>
              );
            })}
            <LoadMoreSentinel
              onLoadMore={() => {
                if (exists(onLoadMore) && !loading) {
                  onLoadMore(inputValue);
                }
              }}
            />
          </List>
        </Menu>
      ) : (
        <span ref={menuPropsRef} /> // To avoid a warning / error by downshift that ref is not applied
      )}
    </SelectContainer>
  );

  const hasTooManyValues = selectedItems.length > MAX_VALUES_LIMIT;

  return (
    <SxLabeled
      {...labelProps}
      ref={clickOutsideRef}
      htmlFor="" // Important to override getLabelProps with this to avoid click events everywhere
      label={
        <>
          {label}
          {tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
      indexPrefix={indexPrefix}
    >
      {hasTooManyValues && value.length > 0 && (
        <TooManyValues count={value.length} onClear={() => onChange([])} />
      )}
      {!hasTooManyValues && !onResolve && Select}
      {!hasTooManyValues && !!onResolve && onDropFile && (
        <SxInputMultiSelectDropzone disabled={disabled} onDropFile={onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </SxLabeled>
  );
};

export default InputMultiSelectTwo;
