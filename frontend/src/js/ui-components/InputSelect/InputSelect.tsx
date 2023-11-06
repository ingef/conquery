import { faChevronDown, faTimes } from "@fortawesome/free-solid-svg-icons";
import { useCombobox } from "downshift";
import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import { exists } from "../../common/helpers/exists";
import { useClickOutside } from "../../common/helpers/useClickOutside";
import { usePrevious } from "../../common/helpers/usePrevious";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Labeled from "../Labeled";
import SelectEmptyPlaceholder from "../SelectEmptyPlaceholder";

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
  SxSelectListOption,
  VerticalSeparator,
} from "./InputSelectComponents";
import { optionMatchesQuery } from "./optionMatchesQuery";

function filterOptions(
  options: SelectOptionT[],
  query: string,
  sortOptions?: (a: SelectOptionT, b: SelectOptionT, query: string) => number,
) {
  const filtered = options.filter((option) =>
    optionMatchesQuery(option, query),
  );

  return sortOptions
    ? filtered.sort((a, b) => sortOptions(a, b, query))
    : filtered;
}

const InputSelect = ({
  dataTestId,
  options,
  placeholder,
  label,
  tooltip,
  indexPrefix,
  disabled,
  clearable,
  className,
  value,
  optional,
  smallMenu,
  onChange,
  sortOptions,
}: {
  label?: string;
  disabled?: boolean;
  options: SelectOptionT[];
  tooltip?: string;
  indexPrefix?: number;
  placeholder?: string;
  clearable?: boolean;
  smallMenu?: boolean;
  className?: string;
  dataTestId?: string;
  value: SelectOptionT | null;
  optional?: boolean;
  onChange: (value: SelectOptionT | null) => void;
  sortOptions?: (a: SelectOptionT, b: SelectOptionT, query: string) => number;
}) => {
  const { t } = useTranslation();
  const previousValue = usePrevious(value);
  const previousOptions = usePrevious(options);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const menuContainerRef = useRef<HTMLDivElement | null>(null);

  const [filteredOptions, setFilteredOptions] = useState(() => {
    if (!value) return options;

    return options.some((option) => option.value === value?.value)
      ? options
      : [value, ...options];
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
    selectedItem,
    selectItem,
    reset: resetComboboxState,
    inputValue,
    setInputValue,
  } = useCombobox({
    itemToString: (item) => {
      if (!item) return "";

      return item.selectedLabel || item.label || String(item.value);
    },
    defaultSelectedItem: value,
    items: filteredOptions,
    stateReducer: (state, { type, changes }) => {
      // This modifies the action payload itself
      // in that way
      // - the default behavior may be adjusted
      // - including the `onStateChange` reactions that diverge from default behavior (see below)
      switch (type) {
        case useCombobox.stateChangeTypes.FunctionReset:
          return {
            ...changes,
            // For some reason, changes doesn't have selectedItem === null
            // so we need to explicitly set null here if clearable
            selectedItem: clearable ? null : state.selectedItem,
          };
        case useCombobox.stateChangeTypes.InputBlur:
          if (changes.selectedItem?.disabled) {
            return state;
          }
          return changes;
        default:
          return changes;
      }
    },
    onInputValueChange: (changes) => {
      if (changes.highlightedIndex !== 0) {
        setHighlightedIndex(0);
      }
    },
    onStateChange: ({ type, ...changes }) => {
      // This only modifies the behavior of some of the actions, after the state has been changed
      switch (type) {
        case useCombobox.stateChangeTypes.InputKeyDownEscape:
          if (changes.isOpen) {
            // Sometimes closing the menu on esc didn't work, this fixes it
            toggleMenu();
          }
          break;
        case useCombobox.stateChangeTypes.InputKeyDownEnter:
        case useCombobox.stateChangeTypes.ItemClick:
        case useCombobox.stateChangeTypes.InputBlur:
          setFilteredOptions(options);

          if (changes.selectedItem) {
            onChange(changes.selectedItem);
          }
          break;
        default:
          break;
      }
    },
  });

  const previousInputValue = usePrevious(inputValue);

  const { ref: menuPropsRef, ...menuProps } = getMenuProps();
  const { ref: inputPropsRef, ...inputProps } = getInputProps();
  const labelProps = getLabelProps();

  const handleBlur = useCallback(() => {
    if (!!selectedItem && inputValue !== selectedItem.label) {
      setInputValue(selectedItem.label);
    }
  }, [inputValue, setInputValue, selectedItem]);

  const clickOutsideRef = useRef<HTMLLabelElement>(null);
  useClickOutside(
    clickOutsideRef,
    useCallback(() => {
      if (isOpen) {
        toggleMenu();
        handleBlur();
      }
    }, [isOpen, toggleMenu, handleBlur]),
  );

  useEffect(
    function takeValueFromAbove() {
      if (
        exists(value) &&
        previousValue !== value &&
        value.value !== selectedItem?.value
      ) {
        selectItem(value);
      }
    },
    [previousValue, selectedItem, selectItem, value],
  );

  useEffect(
    function takeOptionsFromAbove() {
      const previousOptionsLength = previousOptions
        ? previousOptions.length
        : 0;

      if (options.length !== previousOptionsLength) {
        if (inputValue === value?.label) {
          setFilteredOptions(options);
        } else {
          setFilteredOptions(filterOptions(options, inputValue, sortOptions));
        }
      }
    },
    [inputValue, value, options, previousOptions, sortOptions],
  );

  useEffect(
    function filterOptionsOnChangingInput() {
      if (exists(inputValue) && inputValue !== previousInputValue) {
        if (inputValue !== selectedItem?.label) {
          setFilteredOptions(filterOptions(options, inputValue, sortOptions));
        } else {
          setFilteredOptions(options);
        }
      }
    },
    [inputValue, previousInputValue, options, selectedItem, sortOptions],
  );

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
      className={exists(label) ? undefined : className}
      data-test-id={dataTestId}
    >
      <Control disabled={disabled}>
        <ItemsInputContainer>
          <Input
            {...inputProps}
            onBlur={(e) => {
              handleBlur(); // Because sometimes inputProps.onBlur doesn't trigger InputBlur action
              inputProps.onBlur(e);
            }}
            ref={(instance) => {
              inputPropsRef(instance);
              inputRef.current = instance;
            }}
            type="text"
            spellCheck={false}
            disabled={disabled}
            placeholder={placeholder || t("inputSelect.placeholder")}
            onFocus={(e) => {
              e.target.select();
            }}
            onClick={(e) => {
              inputProps.onClick?.(e);
              toggleMenu();
            }}
            onChange={(e) => {
              inputProps.onChange?.(e);
              setInputValue(e.target.value);
            }}
          />
        </ItemsInputContainer>
        {clearable && (inputValue.length > 0 || exists(selectedItem)) && (
          <ResetButton
            icon={faTimes}
            disabled={disabled}
            onClick={() => {
              resetComboboxState();
              if (clearable) {
                onChange(null);
              }
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
          <Menu
            {...menuProps}
            onMouseDown={(e) => {
              // To prevent causing input blur too soon, when selecting an option
              // otherwise, input blur resets the search, which resets the filtered options list
              // leading to a wrong click behavior when filtered
              e.stopPropagation();
            }}
            ref={(instance) => menuPropsRef(instance)}
          >
            <List small={smallMenu}>
              {filteredOptions.length === 0 && <SelectEmptyPlaceholder />}
              {filteredOptions.map((option, index) => {
                const { ref: itemPropsRef, ...itemProps } = getItemProps({
                  index,
                  item: filteredOptions[index],
                });

                return (
                  <SxSelectListOption
                    key={`${option.value}`}
                    option={option}
                    active={
                      highlightedIndex === index ||
                      selectedItem?.value === option.value
                    }
                    {...itemProps}
                    ref={(instance) => {
                      itemPropsRef(instance);
                    }}
                  />
                );
              })}
            </List>
          </Menu>
        </MenuContainer>
      ) : (
        <span ref={menuPropsRef} /> // To avoid a warning / error by downshift that ref is not applied
      )}
    </SelectContainer>
  );

  return label ? (
    <Labeled
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
      className={className}
      optional={optional}
    >
      {Select}
    </Labeled>
  ) : (
    Select
  );
};

export default InputSelect;
