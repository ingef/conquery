import { useEffect } from "react";

import { SelectOptionT } from "../../api/types";
import { getUniqueFileRows } from "../../common/helpers";
import { usePrevious } from "../../common/helpers/usePrevious";

export const useResolvableSelect = ({
  defaultValue,
  onResolve,
}: {
  defaultValue?: SelectOptionT[];
  onResolve?: (
    csvFileLines: string[],
    options?: { showModal?: boolean },
  ) => Promise<void>;
}) => {
  const previousDefaultValue = usePrevious(defaultValue);

  useEffect(
    function resolveDefault() {
      if (!onResolve) {
        return;
      }

      const hasDefaultValueToLoad =
        defaultValue &&
        defaultValue.length > 0 &&
        JSON.stringify(defaultValue) !== JSON.stringify(previousDefaultValue);

      if (hasDefaultValueToLoad) {
        onResolve(
          defaultValue.map((v) => String(v.value)),
          { showModal: false },
        );
      }
    },
    [onResolve, previousDefaultValue, defaultValue],
  );

  const onDropFile = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    if (onResolve) {
      onResolve(rows, { showModal: true });
    }
  };

  return { onDropFile: onResolve ? onDropFile : undefined };
};
