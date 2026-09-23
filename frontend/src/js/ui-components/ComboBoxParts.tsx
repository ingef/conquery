import {
  type ContextType,
  type PointerEvent,
  type RefObject,
  useContext,
  useEffect,
} from "react";
import {
  type ComboBoxState,
  ComboBoxStateContext,
} from "react-aria-components";
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

/**
 * the input's pointer handling: the list opens on focus, a press into the
 * already focused input opens it again, and a drag onto an option picks it
 */
export const inputPointerHandlers = (stateRef: ComboBoxStateRef) => ({
  onPointerDown: (e: PointerEvent<HTMLInputElement>) => {
    const state = stateRef.current;
    if (e.button !== 0 || e.currentTarget.disabled || !state || state.isOpen) {
      return;
    }
    state.open(null, "manual");
  },
  onPointerMove: (e: PointerEvent<HTMLInputElement>) => {
    const state = stateRef.current;
    if (!state || e.pointerType !== "mouse" || e.buttons !== 1) return;
    const key = optionKeyUnder(state, e);
    if (key !== null && key !== state.selectionManager.focusedKey) {
      state.selectionManager.setFocused(true);
      state.selectionManager.setFocusedKey(key);
    }
  },
  onPointerUp: (e: PointerEvent<HTMLInputElement>) => {
    const state = stateRef.current;
    if (!state || e.pointerType !== "mouse" || e.button !== 0) return;
    const key = optionKeyUnder(state, e);
    if (key !== null) state.selectionManager.select(key);
  },
});

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
