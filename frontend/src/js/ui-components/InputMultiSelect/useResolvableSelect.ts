import { useEffect } from "react";

import { SelectOptionT } from "../../api/types";
import { getUniqueFileRows } from "../../common/helpers";
import { usePrevious } from "../../common/helpers/usePrevious";

export const useResolvableSelect = ({
  defaultValue,
  onResolve,
}: {
  defaultValue?: SelectOptionT[];
  onResolve?: (csvFileLines: string[]) => void;
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
        onResolve(defaultValue.map((v) => String(v.value)));
      }
    },
    [onResolve, previousDefaultValue, defaultValue],
  );

  const onDropFile = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    if (onResolve) {
      onResolve(rows);
    }
  };

  return { onDropFile: onResolve ? onDropFile : undefined };
};
