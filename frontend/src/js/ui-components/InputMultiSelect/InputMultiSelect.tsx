import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { Fragment, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { useDebounce } from "../../common/helpers/useDebounce";
import FaIcon from "../../icon/FaIcon";
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
  SxSelectListOption,
  VerticalSeparator,
} from "../InputSelect/InputSelectComponents";
import Labeled from "../Labeled";
import EmptyPlaceholder from "../SelectEmptyPlaceholder";
import TooManyValues from "../TooManyValues";

import LoadMoreSentinel from "./LoadMoreSentinel";
import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";
import { useCloseOnClickOutside } from "./useCloseOnClickOutside";
import { useFilteredOptions } from "./useFilteredOptions";
import { useLoadMoreInitially } from "./useLoadMoreInitially";
import { useResolvableSelect } from "./useResolvableSelect";
import { useSyncWithValueFromAbove } from "./useSyncWithValueFromAbove";

const MAX_SELECTED_ITEMS_LIMIT = 200;

const SENTINEL_INSERT_INDEX_FROM_BOTTOM = 10;

const getSentinelInsertIndex = (optionsLength: number) => {
  if (optionsLength < SENTINEL_INSERT_INDEX_FROM_BOTTOM) {
    return optionsLength - 1;
  }

  return optionsLength - SENTINEL_INSERT_INDEX_FROM_BOTTOM;
};

const SxInputMultiSelectDropzone = styled(InputMultiSelectDropzone)`
  display: block;
`;

const SxFaIcon = styled(FaIcon)`
  margin: 3px 6px;
`;

interface Props {
  label?: string;
  className?: string;
  disabled?: boolean;
  options: SelectOptionT[];
  total?: number;
  tooltip?: string;
  indexPrefix?: number;
  creatable?: boolean;
  value: SelectOptionT[];
  defaultValue?: SelectOptionT[];
  placeholder?: string;
  autoFocus?: boolean;
  onChange: (value: SelectOptionT[]) => void;
  loading?: boolean;
  onResolve?: (csvFileLines: string[]) => void; // The assumption is that this will somehow update `options`
  onLoadMore?: (inputValue: string, config?: { shouldReset?: boolean }) => void;
  onLoadAndInsertAll?: (inputValue: string) => void;
}

const InputMultiSelect = ({
  options,
  className,
  label,
  total,
  tooltip,
  indexPrefix,
  creatable,
  disabled,
  value,
  defaultValue,
  placeholder,
  autoFocus,
  onChange,
  loading,
  onResolve,
  onLoadMore,
  onLoadAndInsertAll,
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
    initialSelectedItems: defaultValue || [],
    onSelectedItemsChange: (changes) => {
      if (changes.selectedItems) {
        onChange(changes.selectedItems);
      }
    },
  });

  useDebounce(
    () => {
      if (onLoadMore && !loading) {
        onLoadMore(inputValue, { shouldReset: true });
      }
    },
    200,
    [inputValue],
  );

  const filteredOptions = useFilteredOptions({
    options,
    selectedItems,
    inputValue,
    creatable,
    skipQueryMatching: !!onLoadMore,
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

  useLoadMoreInitially({ onLoadMore, isOpen, optionsLength: options.length });

  const { ref: menuPropsRef, ...menuProps } = getMenuProps();
  const { ref: inputPropsRef, ...inputProps } = getInputProps(
    getDropdownProps({ autoFocus }),
  );
  const { ref: comboboxRef, ...comboboxProps } = getComboboxProps();
  const labelProps = getLabelProps({});

  const inputRef = useRef<HTMLInputElement | null>(null);

  const clickOutsideRef = useCloseOnClickOutside({ isOpen, toggleMenu });

  useSyncWithValueFromAbove({
    inputValueFromAbove: value,
    selectedItems,
    setSelectedItems,
  });

  const clearStaleSearch = () => {
    if (!isOpen) {
      setInputValue("");
    }
  };

  const filterOptionsCount =
    creatable && inputValue.length > 0
      ? filteredOptions.length - 1
      : filteredOptions.length;

  const Select = (
    <SelectContainer
      onBlur={clearStaleSearch}
      ref={(instance) => {
        if (!label) {
          clickOutsideRef.current = instance;
        }
      }}
    >
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
            onFocus={() => {
              if (inputRef.current) {
                inputRef.current.select();
              }
            }}
            {...inputProps}
            ref={(instance) => {
              inputRef.current = instance;
              inputPropsRef(instance);
            }}
            disabled={disabled}
            spellCheck={false}
            placeholder={
              selectedItems.length > 0
                ? null
                : placeholder
                ? placeholder
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
        {loading && <SxFaIcon icon="spinner" />}
        {!loading && (inputValue.length > 0 || selectedItems.length > 0) && (
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
            total={total}
            optionsCount={filterOptionsCount}
            onInsertAllClick={() => {
              const moreInsertableThanCurrentlyLoaded =
                exists(total) && total > filterOptionsCount;

              if (!!onLoadAndInsertAll && moreInsertableThanCurrentlyLoaded) {
                onLoadAndInsertAll(inputValue);
              } else {
                const optionsWithoutCreatable =
                  creatable && inputValue.length > 0
                    ? filteredOptions.slice(1)
                    : filteredOptions;

                setSelectedItems(optionsWithoutCreatable);
                setInputValue("");
              }
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
                <Fragment key={`${option.value}${option.label}`}>
                  <SxSelectListOption
                    active={highlightedIndex === index}
                    option={option}
                    {...itemProps}
                    ref={itemPropsRef}
                  />
                  {index === getSentinelInsertIndex(filteredOptions.length) &&
                    exists(onLoadMore) && (
                      <LoadMoreSentinel
                        onLoadMore={() => {
                          if (!loading) {
                            onLoadMore(inputValue);
                          }
                        }}
                      />
                    )}
                </Fragment>
              );
            })}
          </List>
        </Menu>
      ) : (
        <span ref={menuPropsRef} /> // To avoid a warning / error by downshift that ref is not applied
      )}
    </SelectContainer>
  );

  const hasTooManyValues = selectedItems.length > MAX_SELECTED_ITEMS_LIMIT;

  const children = (
    <>
      {hasTooManyValues && value.length > 0 && (
        <TooManyValues count={value.length} onClear={() => onChange([])} />
      )}
      {!hasTooManyValues && !onResolve && Select}
      {!hasTooManyValues && !!onResolve && onDropFile && (
        <SxInputMultiSelectDropzone disabled={disabled} onDropFile={onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </>
  );

  return label ? (
    <Labeled
      {...labelProps}
      className={className}
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
      {children}
    </Labeled>
  ) : (
    children
  );
};

export default InputMultiSelect;
