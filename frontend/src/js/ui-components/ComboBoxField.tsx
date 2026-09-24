import { ChevronDownIcon } from "lucide-react";
import { type PointerEvent, useContext, useMemo, useState } from "react";
import {
  type ComboBoxState,
  ComboBoxStateContext,
  type Key,
  ListBox,
  ListBoxItem,
  ComboBox as RacComboBox,
} from "react-aria-components";
import ReactMarkdown from "react-markdown";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { Input, InputButton, type InputProps } from "./Input";
import { optionMatchesQuery } from "./InputMultiSelect/optionMatchesQuery";
import { type FieldLabelProps, Label } from "./Label";
import { Popover } from "./Popover";
import SelectEmptyPlaceholder from "./SelectEmptyPlaceholder";

const listBox = tv({
  base: [
    "max-h-[300px]",
    "overflow-y-auto overscroll-contain",
    "p-[3px]",
    "outline-none",
  ],
});

const listBoxItem = tv({
  base: [
    "mb-[2px]",
    "px-2 py-[3px]",
    "cursor-pointer",
    // explicit: through the portal an item would inherit body's styles
    "text-base font-light text-gray-800",
    "outline-none",
    "transition-[background-color] duration-100",
    "data-focused:bg-primary-50 data-selected:bg-primary-50",
    "data-disabled:cursor-not-allowed data-disabled:opacity-40",
    // react-markdown wraps the label in a paragraph
    "[&_p]:m-0",
  ],
});

// Press the input, drag onto an option, release: react-aria selects on the option's
// own pointerup and focuses it on hover, which works in Chrome. Firefox delivers a
// mouse drag that starts in a text input only to that input, so the input does the
// same by pointer position; where the option gets the events itself, nothing is under
// the pointer here and these handlers stay idle
const optionKeyUnder = (state: ComboBoxState<object>, e: PointerEvent) => {
  const option = document
    .elementFromPoint(e.clientX, e.clientY)
    ?.closest<HTMLElement>('[role="option"][data-key]');
  const key = [...state.collection.getKeys()].find(
    (k) => String(k) === option?.dataset.key,
  );

  return key !== undefined && !state.selectionManager.isDisabled(key)
    ? key
    : null;
};

// the list opens on focus; a press into the already focused input opens it again
const ComboBoxInput = (props: InputProps) => {
  const state = useContext(ComboBoxStateContext);

  return (
    <Input
      {...props}
      onPointerDown={(e) => {
        if (e.button === 0 && state && !state.isOpen)
          state.open(null, "manual");
      }}
      onPointerMove={(e) => {
        if (!state || e.pointerType !== "mouse" || e.buttons !== 1) return;
        const key = optionKeyUnder(state, e);
        if (key !== null && key !== state.selectionManager.focusedKey) {
          state.selectionManager.setFocused(true);
          state.selectionManager.setFocusedKey(key);
        }
      }}
      onPointerUp={(e) => {
        if (!state || e.pointerType !== "mouse" || e.button !== 0) return;
        const key = optionKeyUnder(state, e);
        if (key !== null) state.selectionManager.select(key);
      }}
    />
  );
};

const textOf = (option: SelectOptionT | null) =>
  option ? option.selectedLabel || option.label || String(option.value) : "";

export type ComboBoxFieldProps = FieldLabelProps & {
  options: SelectOptionT[];
  value: SelectOptionT | null;
  /** null when the text is cleared */
  onChange: (value: SelectOptionT | null) => void;
  placeholder?: string;
  indexPrefix?: number;
  /** a help icon after the label */
  tooltip?: string;
  /** shown below the input; the field is invalid while it is set */
  errorMessage?: string;
  isDisabled?: boolean;
  autoFocus?: boolean;
  /** orders the options that match the typed text */
  sortOptions?: (a: SelectOptionT, b: SelectOptionT, query: string) => number;
  "data-test-id"?: string;
};

/**
 * A single choice from a list, typed to filter: react-aria's ComboBox with a
 * list box in a popover. Options match on label and value.
 */
export const ComboBoxField = ({
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  options,
  value,
  onChange,
  sortOptions,
  ...props
}: ComboBoxFieldProps) => {
  const [query, setQuery] = useState(() => textOf(value));

  const allOptions = useMemo(
    () =>
      value && !options.some((option) => option.value === value.value)
        ? [value, ...options]
        : options,
    [options, value],
  );

  const items = useMemo(() => {
    if (!query || query === textOf(value)) return allOptions;

    const matching = allOptions.filter((option) =>
      optionMatchesQuery(option, query),
    );

    return sortOptions
      ? matching.sort((a, b) => sortOptions(a, b, query))
      : matching;
  }, [allOptions, query, value, sortOptions]);

  const onSelectionChange = (key: Key | null) =>
    onChange(
      key === null
        ? null
        : (allOptions.find((option) => option.value === key) ?? null),
    );

  return (
    <RacComboBox
      className="min-w-0"
      menuTrigger="focus"
      allowsEmptyCollection
      validationBehavior="aria"
      isInvalid={exists(errorMessage)}
      items={items}
      selectedKey={value?.value ?? null}
      onSelectionChange={onSelectionChange}
      onInputChange={setQuery}
      {...props}
    >
      {({ isDisabled, isInvalid }) => (
        <>
          {label && (
            <Label indexPrefix={indexPrefix} tooltip={tooltip}>
              {label}
            </Label>
          )}
          <ComboBoxInput
            placeholder={placeholder}
            isDisabled={isDisabled}
            isInvalid={isInvalid}
            addonRight={
              <InputButton data-test-id="selection-dropdown">
                <ChevronDownIcon />
              </InputButton>
            }
          />
          <FieldError>{errorMessage}</FieldError>
          <Popover className="w-(--trigger-width)">
            <ListBox
              className={listBox()}
              data-test-id="select-options"
              renderEmptyState={() => <SelectEmptyPlaceholder />}
            >
              {(option: SelectOptionT) => (
                <ListBoxItem
                  id={option.value}
                  textValue={textOf(option)}
                  isDisabled={option.disabled}
                  className={listBoxItem()}
                >
                  {option.displayLabel ?? (
                    <ReactMarkdown>
                      {option.label || String(option.value)}
                    </ReactMarkdown>
                  )}
                </ListBoxItem>
              )}
            </ListBox>
          </Popover>
        </>
      )}
    </RacComboBox>
  );
};
