import { useEffect } from "react";

import { SelectOptionT } from "../../api/types";
import { getUniqueFileRows } from "../../common/helpers/fileHelper";
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
    // TODO: Check, if we really need this effect,
    // since resolving default values (that are part of a big multi select filter
    // inside of an edited query node) is also taken care of by FilterListMultiSelect
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
