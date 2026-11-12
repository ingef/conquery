import {
  CheckIcon,
  ChevronDownIcon,
  LoaderCircleIcon,
  XIcon,
} from "lucide-react";
import { useMemo, useRef, useState } from "react";
import {
  ButtonContext,
  Collection,
  ComboBoxValue,
  Group,
  type Key,
  ListBox,
  ListBoxItem,
  ListBoxLoadMoreItem,
  ListStateContext,
  Button as RacButton,
  ComboBox as RacComboBox,
  Input as RacInput,
  Tag,
  TagGroup,
  TagList,
} from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import { getFileRows } from "../common/helpers/fileHelper";
import { useDebounce } from "../common/helpers/useDebounce";
import { Button, buttonStyle } from "./Button";
import {
  ComboBoxStateBridge,
  type ComboBoxStateRef,
  inputPointerHandlers,
  listBox,
  listBoxItem,
  OptionLabel,
  optionMatchesQuery,
  optionText,
  useFocusFirstOption,
  withoutDuplicates,
} from "./ComboBoxParts";
import DropzoneWithFileInput from "./DropzoneWithFileInput";
import { FieldError } from "./FieldError";
import { InputAddons, InputButton, inputControl, inputFrame } from "./Input";
import { type FieldLabelProps, Label } from "./Label";
import { Popover } from "./Popover";
import SelectEmptyPlaceholder from "./SelectEmptyPlaceholder";
import { C3, textStyle } from "./Typography";

// above this the tags collapse into one that only clears them all
const MAX_TAGS = 20;
const SUMMARY_TAG = "__summary__";

// tags and the input share the lines inside the frame
const content = tv({
  base: ["flex grow flex-wrap items-center", "gap-1", "min-w-0", "px-2 py-0.5"],
});

const tag = tv({
  base: [
    "flex items-center",
    "h-5 gap-0.5",
    "rounded",
    "bg-gray-50",
    "px-[5px]",
    textStyle({ size: 2, tone: "default" }),
    "shadow-[0.5px_0.5px_1px_0_rgb(0_0_0/20%),inset_0_0_0_1px_#ccc]",
    "outline-none",
    "data-focus-visible:outline-2 data-focus-visible:outline-primary-500",
    // react-markdown wraps the label in a paragraph
    "[&_p]:m-0",
  ],
});

// the tertiary icon button, shrunk to the chip's line where the sm size would stick out
const tagRemove = buttonStyle({
  intent: "tertiary",
  size: "sm",
  iconOnly: true,
  class: "size-4 rounded-sm hover:bg-gray-100 [&_svg]:[--icon-size:12px]",
});

const actionBar = tv({
  base: [
    "flex items-center justify-between",
    "gap-2",
    "px-[10px] py-[5px]",
    "border-b border-gray-100",
  ],
});

const loadingRow = tv({
  base: ["flex justify-center", "py-[3px]", "text-gray-600"],
});

export type ComboBoxMultiFieldProps = FieldLabelProps & {
  options: SelectOptionT[];
  value: SelectOptionT[];
  onChange: (value: SelectOptionT[]) => void;
  placeholder?: string;
  indexPrefix?: number;
  /** a help icon after the label */
  tooltip?: string;
  /** shown below the input; the field is invalid while it is set */
  errorMessage?: string;
  isDisabled?: boolean;
  autoFocus?: boolean;
  /** typed text becomes a value of its own */
  creatable?: boolean;
  maxInputLength?: number;
  /**
   * the options are a page of a server search: nothing is filtered here,
   * the next page loads on scroll, page one reloads on typing and on open
   */
  onLoadMore?: (query: string, config?: { shouldReset?: boolean }) => void;
  /** inserts every match of the server search, not just the loaded page */
  onLoadAndInsertAll?: (query: string) => void;
  /** how many options the server search matches */
  total?: number;
  loading?: boolean;
  /** the lines of a dropped or pasted file become values */
  onResolve?: (lines: string[]) => void;
  "data-test-id"?: string;
};

// react-aria compares the selected keys by reference and resets the typed
// text when they change, so the array only changes with its content
const useSelectedKeys = (value: SelectOptionT[]) => {
  const keysRef = useRef<Key[]>([]);
  const keys = value.map((item) => item.value);
  const same =
    keys.length === keysRef.current.length &&
    keys.every((key, i) => key === keysRef.current[i]);
  if (!same) keysRef.current = keys;
  return keysRef.current;
};

/**
 * Several choices from a list, typed to filter: react-aria's ComboBox in
 * multiple selection mode, the chosen values as tags in the frame.
 */
export const ComboBoxMultiField = ({
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  options,
  value,
  onChange,
  creatable,
  maxInputLength,
  onLoadMore,
  onLoadAndInsertAll,
  total,
  loading,
  onResolve,
  ...props
}: ComboBoxMultiFieldProps) => {
  const { t } = useTranslation();
  const [query, setQuery] = useState("");
  const [isOpen, setOpen] = useState(false);
  const stateRef: ComboBoxStateRef = useRef(null);
  const selectedKeys = useSelectedKeys(value);
  const collapsed = value.length > MAX_TAGS;
  const tags: SelectOptionT[] = collapsed
    ? [
        {
          value: SUMMARY_TAG,
          label: t("inputMultiSelect.selectedCount", { count: value.length }),
        },
      ]
    : value;

  // not for a value that is already selected: react-aria would toggle it off
  const createRow = useMemo(
    () =>
      creatable &&
      query &&
      !options.some((o) => String(o.value) === query) &&
      !value.some((item) => String(item.value) === query)
        ? { value: query, label: `${t("common.create")}: "${query}"` }
        : null,
    [creatable, query, options, value, t],
  );

  const listed = useMemo(
    () =>
      onLoadMore
        ? options
        : options.filter((option) => optionMatchesQuery(option, query)),
    [options, query, onLoadMore],
  );

  const items = useMemo(
    () => (createRow ? [createRow, ...listed] : listed),
    [createRow, listed],
  );

  useFocusFirstOption(stateRef, query, items);

  useDebounce(
    () => {
      if (onLoadMore && isOpen && !loading) {
        onLoadMore(query, { shouldReset: true });
      }
    },
    350,
    [query, isOpen],
  );

  const onSelectionChange = (keys: Key[]) => {
    const selected = new Set(keys);
    const kept = value.filter((item) => selected.has(item.value));
    const existing = new Set(value.map((item) => item.value));
    const added = [...selected]
      .filter((key) => !existing.has(key))
      .map((key) =>
        createRow && key === createRow.value
          ? { value: query, label: query }
          : listed.find((option) => option.value === key),
      )
      .filter(exists);

    onChange([...kept, ...added]);

    if (added.some((option) => option.value === query)) {
      stateRef.current?.setInputValue("");
    }
  };

  const onInsertAll = () => {
    if (onLoadAndInsertAll && exists(total) && total > listed.length) {
      onLoadAndInsertAll(query);
    } else {
      onChange(withoutDuplicates(value, listed));
      stateRef.current?.setInputValue("");
    }
  };

  const clear = () => {
    onChange([]);
    stateRef.current?.setInputValue("");
  };

  const defaultPlaceholder = onResolve
    ? t("inputMultiSelect.dndPlaceholder")
    : t("inputSelect.placeholder");

  const field = (
    <RacComboBox
      className="w-full min-w-0"
      selectionMode="multiple"
      menuTrigger="focus"
      allowsEmptyCollection
      validationBehavior="aria"
      isInvalid={exists(errorMessage)}
      items={items}
      value={selectedKeys}
      onChange={onSelectionChange}
      onInputChange={setQuery}
      onOpenChange={setOpen}
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
          <Group
            className={inputFrame({ multiline: true })}
            isDisabled={isDisabled}
            isInvalid={isInvalid}
          >
            <div className={content()}>
              {/* ComboBoxValue skips the combobox's hidden pass that collects its options, which would otherwise swallow the tags; the tag list must not see the combobox's list state either */}
              <ComboBoxValue className="contents">
                {() => (
                  <ListStateContext.Provider value={null}>
                    <TagGroup
                      className="contents"
                      aria-label={t("inputMultiSelect.selectedValues")}
                      disabledKeys={
                        isDisabled ? tags.map((item) => item.value) : []
                      }
                      onRemove={(keys) =>
                        onChange(
                          collapsed
                            ? []
                            : value.filter((item) => !keys.has(item.value)),
                        )
                      }
                    >
                      <TagList className="contents" items={tags}>
                        {(item) => (
                          <Tag
                            id={item.value}
                            textValue={optionText(item)}
                            className={tag()}
                          >
                            <OptionLabel option={item} />
                            <RacButton slot="remove" className={tagRemove}>
                              <XIcon />
                            </RacButton>
                          </Tag>
                        )}
                      </TagList>
                    </TagGroup>
                  </ListStateContext.Provider>
                )}
              </ComboBoxValue>
              <RacInput
                className={inputControl({ inline: true })}
                placeholder={
                  value.length > 0
                    ? undefined
                    : (placeholder ?? defaultPlaceholder)
                }
                maxLength={maxInputLength}
                {...inputPointerHandlers(stateRef)}
                onKeyDown={(e) => {
                  if (
                    e.key === "Backspace" &&
                    !query &&
                    value.length > 0 &&
                    !collapsed
                  ) {
                    onChange(value.slice(0, -1));
                  }
                }}
              />
            </div>
            <InputAddons>
              {loading && <LoaderCircleIcon />}
              {!loading && (query || value.length > 0) && (
                // the combobox hands its open behavior to every Button inside; this one clears
                <ButtonContext.Provider value={null}>
                  <InputButton
                    aria-label={t("common.clearValue")}
                    isDisabled={isDisabled}
                    onPress={clear}
                  >
                    <XIcon />
                  </InputButton>
                </ButtonContext.Provider>
              )}
              <InputButton>
                <ChevronDownIcon />
              </InputButton>
            </InputAddons>
          </Group>
          <FieldError>{errorMessage}</FieldError>
          <Popover className="w-(--trigger-width)">
            <div className={actionBar()}>
              <C3 tone="muted">
                {t("inputMultiSelect.options", { count: listed.length })}
                {exists(total) &&
                  total !== listed.length &&
                  t("inputMultiSelect.ofTotal", { count: total })}
              </C3>
              <Button
                intent="secondary"
                size="sm"
                isDisabled={listed.length === 0}
                onPress={onInsertAll}
              >
                {t("inputMultiSelect.insertAll")}
              </Button>
            </div>
            <ListBox
              className={listBox()}
              renderEmptyState={() =>
                creatable ? null : <SelectEmptyPlaceholder />
              }
            >
              <Collection items={items}>
                {(option: SelectOptionT) => (
                  <ListBoxItem
                    id={option.value}
                    textValue={optionText(option)}
                    isDisabled={option.disabled}
                    className={listBoxItem()}
                  >
                    {({ isSelected }) => (
                      <>
                        <span className="min-w-0 grow">
                          <OptionLabel option={option} />
                        </span>
                        {isSelected && <CheckIcon className="shrink-0" />}
                      </>
                    )}
                  </ListBoxItem>
                )}
              </Collection>
              {onLoadMore && (
                <ListBoxLoadMoreItem
                  isLoading={loading}
                  onLoadMore={() => onLoadMore(query)}
                >
                  <div className={loadingRow()}>
                    <LoaderCircleIcon />
                  </div>
                </ListBoxLoadMoreItem>
              )}
            </ListBox>
          </Popover>
        </>
      )}
    </RacComboBox>
  );

  if (!onResolve) return field;

  return (
    <DropzoneWithFileInput
      onDrop={async (item) => {
        if (item.files) {
          onResolve(await getFileRows(item.files[0]));
        }
      }}
      disableClick
      naked
      showImportButton={!props.isDisabled}
      onImportLines={onResolve}
    >
      {() => field}
    </DropzoneWithFileInput>
  );
};
