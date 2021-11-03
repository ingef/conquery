import { useEffect } from "react";

import type { SelectOptionT } from "../../api/types";
import { usePrevious } from "../../common/helpers/usePrevious";

export const useSyncWithValueFromAbove = ({
  inputValueFromAbove,
  selectedItems,
  setSelectedItems,
}: {
  inputValueFromAbove: SelectOptionT[];
  setSelectedItems: (items: SelectOptionT[]) => void;
  selectedItems: SelectOptionT[];
}) => {
  const previousInputValue = usePrevious(inputValueFromAbove);

  useEffect(() => {
    const previousInputValStr = JSON.stringify(previousInputValue);
    const inputValStr = JSON.stringify(inputValueFromAbove);

    if (
      previousInputValStr !== inputValStr &&
      inputValStr !== JSON.stringify(selectedItems)
    ) {
      setSelectedItems(inputValueFromAbove);
    }
  }, [
    previousInputValue,
    selectedItems,
    setSelectedItems,
    inputValueFromAbove,
  ]);
};
