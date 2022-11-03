import { useEffect } from "react";

import type { SelectOptionT } from "../../api/types";
import { usePrevious } from "../../common/helpers/usePrevious";

/**
 * The idea here is that we want to allow parent components to update the `value` state
 * at any time – then the selected items should update accordingly.
 *
 * Unfortunately, it led to an infinite loop, when selecting items fast.
 * 1) every click causes an internal change to `selectedItems`
 * 2) this change is synced up using `onStateChange` from `useMultipleSelection`
 * 3) which triggers a change to value and causes a rerender + this effect to re-run
 * 4) but this effect will already have a new state for selectedItems
 * 5) so we're calling setSelectedItems with an outdated value
 *
 * => To counter this, we introduced a "syncing" boolean, which we set true in 2) and double-check before 5)
 *
 * This works, but there is probably a better way to solve this,
 * like trying to tie `value` to `selectedItems` more directly
 * (= having more of a "controled component state").
 */
export const useSyncWithValueFromAbove = ({
  value,
  selectedItems,
  setSelectedItems,
  syncingState,
  setSyncingState,
}: {
  value: SelectOptionT[];
  setSelectedItems: (items: SelectOptionT[]) => void;
  selectedItems: SelectOptionT[];
  syncingState: boolean;
  setSyncingState: (syncing: boolean) => void;
}) => {
  const prevValue = usePrevious(value);

  useEffect(() => {
    const prevValueStr = JSON.stringify(prevValue);
    const valueStr = JSON.stringify(value);
    const selectedItemsStr = JSON.stringify(selectedItems);

    const valueChanged = prevValueStr !== valueStr;
    const weDontHaveValueAlreadySelected = valueStr !== selectedItemsStr;

    const takeFromAbove = valueChanged && weDontHaveValueAlreadySelected;

    if (syncingState) {
      // Helps prevent race conditions, when selecting options fast
      setSyncingState(false);
      return;
    }

    if (takeFromAbove) {
      setSelectedItems(value);
    }
  }, [
    selectedItems,
    setSelectedItems,
    prevValue,
    value,
    syncingState,
    setSyncingState,
  ]);
};
