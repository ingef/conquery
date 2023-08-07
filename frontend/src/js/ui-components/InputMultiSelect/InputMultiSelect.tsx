import styled from "@emotion/styled";
import {
  faChevronDown,
  faSpinner,
  faTimes,
} from "@fortawesome/free-solid-svg-icons";
import { useCombobox, useMultipleSelection } from "downshift";
import { Fragment, memo, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { useDebounce } from "../../common/helpers/useDebounce";
import FaIcon from "../../icon/FaIcon";
import InfoTooltip from "../../tooltip/InfoTooltip";
import DropzoneWithFileInput from "../DropzoneWithFileInput";
import {
  Control,
  DropdownToggleButton,
  Input,
  ItemsInputContainer,
  List,
  Menu,
  MenuContainer,
  ResetButton,
  SelectContainer,
  VerticalSeparator,
} from "../InputSelect/InputSelectComponents";
import Labeled from "../Labeled";
import EmptyPlaceholder from "../SelectEmptyPlaceholder";
import TooManyValues from "../TooManyValues";

import ListItem from "./ListItem";
import LoadMoreSentinel from "./LoadMoreSentinel";
import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";
import { useCloseOnClickOutside } from "./useCloseOnClickOutside";
import { useFilteredOptions } from "./useFilteredOptions";
import { useLoadMoreInitially } from "./useLoadMoreInitially";
import { useResolvableSelect } from "./useResolvableSelect";

const MAX_SELECTED_ITEMS_LIMIT = 200;

const SENTINEL_INSERT_INDEX_FROM_BOTTOM = 10;

const getSentinelInsertIndex = (optionsLength: number) => {
  if (optionsLength < SENTINEL_INSERT_INDEX_FROM_BOTTOM) {
    return optionsLength - 1;
  }

  return optionsLength - SENTINEL_INSERT_INDEX_FROM_BOTTOM;
};

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
  useResolvableSelect({
    defaultValue,
    onResolve,
  });

  const menuContainerRef = useRef<HTMLDivElement | null>(null);
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
    selectedItems: value,
    onStateChange: (changes) => {
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
          // Support disabled items
          if (changes.selectedItem?.disabled) {
            return state;
          }

          // Make sure we're staying around the index of the item that was just selected
          const stayAlmostAtTheSamePositionIndex =
            state.highlightedIndex === filteredOptions.length - 1
              ? state.highlightedIndex - 1
              : state.highlightedIndex;

          // Determine the right item to be "chosen", supporting "creatable" items
          const hasChosenCreatableItem =
            creatable && state.highlightedIndex === 0 && inputValue.length > 0;

          const selectedItem = hasChosenCreatableItem
            ? { value: inputValue, label: inputValue }
            : changes.selectedItem;

          const hasItemHighlighted = state.highlightedIndex > -1;
          const isNotSelectedYet =
            !!selectedItem &&
            !selectedItems.find((item) => selectedItem.value === item.value);

          if (isNotSelectedYet && hasItemHighlighted) {
            addSelectedItem(selectedItem);
          }

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
  const labelProps = getLabelProps({});

  const inputRef = useRef<HTMLInputElement | null>(null);

  const clickOutsideRef = useCloseOnClickOutside({ isOpen, toggleMenu });

  const clearStaleSearch = () => {
    if (!isOpen) {
      setInputValue("");
    }
  };

  const filterOptionsCount =
    creatable && inputValue.length > 0
      ? filteredOptions.length - 1
      : filteredOptions.length;

  useEffect(
    function scrollIntoView() {
      if (isOpen) {
        menuContainerRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "nearest",
        });
      }
    },
    [isOpen],
  );

  const Select = (
    <SelectContainer
      onBlur={clearStaleSearch}
      ref={(instance) => {
        if (!label) {
          clickOutsideRef.current = instance;
        }
      }}
    >
      <Control disabled={disabled}>
        <ItemsInputContainer>
          {selectedItems.map((item, index) => {
            return (
              <SelectedItem
                key={`${item.value}${index}`}
                index={index}
                item={item}
                active={index === activeIndex}
                disabled={disabled}
                getSelectedItemProps={getSelectedItemProps}
                removeSelectedItem={removeSelectedItem}
              />
            );
          })}
          <Input
            type="text"
            value={inputValue}
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
              inputProps.onClick?.(e);
            }}
            onChange={(e) => {
              inputProps.onChange?.(e);
              setInputValue(e.target.value);
            }}
          />
        </ItemsInputContainer>
        {loading && <SxFaIcon icon={faSpinner} />}
        {!loading && (inputValue.length > 0 || selectedItems.length > 0) && (
          <ResetButton
            icon={faTimes}
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
          icon={faChevronDown}
          {...getToggleButtonProps()}
        />
      </Control>
      {isOpen ? (
        <MenuContainer ref={menuContainerRef}>
          <Menu {...menuProps} ref={(instance) => menuPropsRef(instance)}>
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

                  setSelectedItems([
                    ...selectedItems,
                    ...optionsWithoutCreatable,
                  ]);
                  setInputValue("");
                }
              }}
            />
            <List>
              {!creatable && filteredOptions.length === 0 && (
                <EmptyPlaceholder />
              )}
              {filteredOptions.map((option, index) => (
                <Fragment key={`${index}${option.value}${option.label}`}>
                  <ListItem
                    index={index}
                    highlightedIndex={highlightedIndex}
                    item={filteredOptions[index]}
                    getItemProps={getItemProps}
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
              ))}
            </List>
          </Menu>
        </MenuContainer>
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
      {!hasTooManyValues && !!onResolve && (
        <DropzoneWithFileInput
          onDrop={() => {}}
          disableClick
          tight
          importButtonOutside
          showImportButton={!disabled}
          onImportLines={onResolve}
        >
          {() => Select}
        </DropzoneWithFileInput>
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

export default memo(InputMultiSelect);
