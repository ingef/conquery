import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { useCombobox, useMultipleSelection } from "downshift";
import { useIntersectionObserver } from "js/common/useIntersectionObserver";
import { useState, useMemo, useRef, useCallback } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { useClickOutside } from "../../common/helpers/useClickOutside";
import InfoTooltip from "../../tooltip/InfoTooltip";
import InputMultiSelectDropzone from "../InputMultiSelectDropzone";
import Labeled from "../Labeled";
import EmptyPlaceholder from "../SelectEmptyPlaceholder";
import SelectListOption from "../SelectListOption";

import MenuActionBar from "./MenuActionBar";
import SelectedItem from "./SelectedItem";
import { useResolvableSelect } from "./useResolvableSelect";

const Control = styled("div")<{ disabled?: boolean }>`
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: 4px;
  display: flex;
  align-items: center;
  overflow: hidden;
  padding: 3px 3px 3px 8px;
  background-color: white;
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
    `}

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
  height: 20px;
  outline: none;
  flex-grow: 1;
  flex-basis: 30px;
  ${({ disabled }) =>
    disabled &&
    css`
      cursor: not-allowed;
      pointer-events: none;
      &:placehoder {
        opacity: 0.5;
      }
    `}
`;

const SxLabeled = styled(Labeled)`
  padding: 2px;
`;

const DropdownToggleButton = styled(IconButton)`
  padding: 3px 6px;
`;

const ResetButton = styled(IconButton)`
  padding: 3px 8px;
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
  loading?: boolean;
  onResolve?: (csvFileLines: string[]) => void; // The assumption is that this will somehow update `options`
  onLoadMore?: () => void;
}

const InputMultiSelectTwo = ({
  options,
  input,
  label,
  tooltip,
  indexPrefix,
  creatable,
  disabled,
  loading,
  onResolve,
  onLoadMore,
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
              disabled: false,
            },
          ]
        : [];

    const stillSelectable = (option: SelectOptionT) =>
      selectedItems.indexOf(option) < 0;

    const matchesQuery = (option: SelectOptionT) => {
      const lowerInputValue = inputValue.toLowerCase();
      const lowerLabel = option.label.toLowerCase();

      return (
        lowerLabel.includes(lowerInputValue) ||
        String(option.value).toLowerCase().includes(lowerInputValue)
      );
    };

    const regularOptions = options.filter(
      (option) => stillSelectable(option) && matchesQuery(option),
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

  const intersectionObserverRef = useRef<HTMLDivElement | null>(null);
  useIntersectionObserver(
    intersectionObserverRef,
    useCallback(
      (_, isIntersecting) => {
        if (isIntersecting && !loading && onLoadMore) {
          onLoadMore();
        }
      },
      [onLoadMore, loading],
    ),
  );

  const clickOutsideRef = useRef<HTMLLabelElement>(null);
  useClickOutside(
    clickOutsideRef,
    useCallback(() => {
      if (isOpen) {
        toggleMenu();
      }
    }, [isOpen, toggleMenu]),
  );

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
                  ref={(instance) => {
                    itemPropsRef(instance);
                    if (index === filteredOptions.length - 1) {
                      intersectionObserverRef.current = instance;
                    }
                  }}
                >
                  {option.label}
                </SelectListOption>
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
      {!onResolve && Select}
      {onResolve && onDropFile && (
        <SxInputMultiSelectDropzone disabled={disabled} onDropFile={onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </SxLabeled>
  );
};

export default InputMultiSelectTwo;
