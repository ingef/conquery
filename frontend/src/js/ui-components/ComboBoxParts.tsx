import {
  type ContextType,
  type PointerEvent,
  type RefObject,
  useContext,
  useEffect,
} from "react";
import { ComboBoxStateContext } from "react-aria-components";
import ReactMarkdown from "react-markdown";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../api/types";

export const listBox = tv({
  base: [
    "max-h-[300px]",
    "overflow-y-auto overscroll-contain",
    "p-[3px]",
    "outline-none",
  ],
});

export const listBoxItem = tv({
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

export const optionText = (option: SelectOptionT | null) =>
  option ? option.selectedLabel || option.label || String(option.value) : "";

/** the option's label, markdown unless the option brings its own element */
export const OptionLabel = ({ option }: { option: SelectOptionT }) =>
  option.displayLabel ?? (
    <ReactMarkdown>{option.label || String(option.value)}</ReactMarkdown>
  );

export const optionMatchesQuery = (option: SelectOptionT, query?: string) => {
  if (!query || option.alwaysShown) return true;

  const lowerQuery = query.toLowerCase();
  const lowerLabel = option.label.toLowerCase();

  return (
    lowerLabel.includes(lowerQuery) ||
    String(option.value).toLowerCase().includes(lowerQuery)
  );
};

/** the added options after the existing ones, minus those already there */
export const withoutDuplicates = (
  existing: SelectOptionT[],
  added: SelectOptionT[],
) => [
  ...existing,
  ...added.filter(
    (option) => !existing.some((item) => item.value === option.value),
  ),
];

export type ComboBoxStateRef = RefObject<
  ContextType<typeof ComboBoxStateContext>
>;

/** hands react-aria's combobox state to the field around it */
export const ComboBoxStateBridge = ({
  stateRef,
}: {
  stateRef: ComboBoxStateRef;
}) => {
  const state = useContext(ComboBoxStateContext);
  // the combobox renders its children once more in a hidden pass without the state
  if (state) stateRef.current = state;
  return null;
};

/** the list opens on focus; a press into the already focused input opens it again */
export const openOnPress =
  (stateRef: ComboBoxStateRef) => (e: PointerEvent<HTMLInputElement>) => {
    const state = stateRef.current;
    if (e.button !== 0 || e.currentTarget.disabled || !state || state.isOpen) {
      return;
    }
    state.open(null, "manual");
  };

/** while typing, the first option that can be picked is focused, so Enter picks it */
export const useFocusFirstOption = (
  stateRef: ComboBoxStateRef,
  query: string,
  items: SelectOptionT[],
) => {
  // biome-ignore lint/correctness/useExhaustiveDependencies: reruns when the items change, after react-aria has cleared the focus for the new text
  useEffect(() => {
    const state = stateRef.current;
    if (!state || !query) return;

    const { selectionManager, collection } = state;
    if (
      selectionManager.focusedKey != null &&
      collection.getItem(selectionManager.focusedKey)
    ) {
      return;
    }

    let key = collection.getFirstKey();
    while (key != null && selectionManager.isDisabled(key)) {
      key = collection.getKeyAfter(key);
    }
    if (key != null) selectionManager.setFocusedKey(key);
  }, [stateRef, query, items]);
};
