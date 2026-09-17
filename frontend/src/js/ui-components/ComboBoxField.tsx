import { faChevronDown } from "@fortawesome/free-solid-svg-icons";
import { useMemo, useRef, useState } from "react";
import {
  type Key,
  ListBox,
  ListBoxItem,
  ComboBox as RacComboBox,
} from "react-aria-components";

import type { SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import {
  ComboBoxStateBridge,
  type ComboBoxStateRef,
  listBox,
  listBoxItem,
  OptionLabel,
  openOnPress,
  optionMatchesQuery,
  optionText,
  useFocusFirstOption,
} from "./ComboBoxParts";
import { FieldError } from "./FieldError";
import { Icon } from "./Icon";
import { Input, InputButton } from "./Input";
import { type FieldLabelProps, Label } from "./Label";
import { Popover } from "./Popover";
import SelectEmptyPlaceholder from "./SelectEmptyPlaceholder";

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
  const [query, setQuery] = useState(() => optionText(value));
  const stateRef: ComboBoxStateRef = useRef(null);

  const allOptions = useMemo(
    () =>
      value && !options.some((option) => option.value === value.value)
        ? [value, ...options]
        : options,
    [options, value],
  );

  const items = useMemo(() => {
    if (!query || query === optionText(value)) return allOptions;

    const matching = allOptions.filter((option) =>
      optionMatchesQuery(option, query),
    );

    return sortOptions
      ? matching.sort((a, b) => sortOptions(a, b, query))
      : matching;
  }, [allOptions, query, value, sortOptions]);

  // only typed text, not the picked option's text that react-aria writes back
  useFocusFirstOption(
    stateRef,
    query === optionText(value) ? "" : query,
    items,
  );

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
          <ComboBoxStateBridge stateRef={stateRef} />
          {label && (
            <Label indexPrefix={indexPrefix} tooltip={tooltip}>
              {label}
            </Label>
          )}
          <Input
            placeholder={placeholder}
            isDisabled={isDisabled}
            isInvalid={isInvalid}
            onPointerDown={openOnPress(stateRef)}
            addonRight={
              <InputButton data-test-id="selection-dropdown">
                <Icon icon={faChevronDown} />
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
                  textValue={optionText(option)}
                  isDisabled={option.disabled}
                  className={listBoxItem()}
                >
                  <OptionLabel option={option} />
                </ListBoxItem>
              )}
            </ListBox>
          </Popover>
        </>
      )}
    </RacComboBox>
  );
};
