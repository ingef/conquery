import React, { FC, useEffect } from "react";

import { getUniqueFileRows } from "../common/helpers/fileHelper";
import { usePrevious } from "../common/helpers/usePrevious";

import AsyncInputMultiSelect from "./AsyncInputMultiSelect";
import InputMultiSelectOld, {
  InputMultiSelectProps,
} from "./InputMultiSelectOld";

interface PropsT extends InputMultiSelectProps {
  onLoad?: (prefix: string) => void;
  onResolve: (rows: string[]) => void;
}

const MultiSelectWithFileSupport: FC<PropsT> = ({
  input,
  onLoad,
  onResolve,
  ...props
}) => {
  const previousDefaultValue = usePrevious(input.defaultValue);

  useEffect(
    function resolveDefault() {
      async function resolveDefaultValue() {
        const hasDefaultValueToLoad =
          input.defaultValue &&
          input.defaultValue.length > 0 &&
          JSON.stringify(input.defaultValue) !==
            JSON.stringify(previousDefaultValue);

        if (hasDefaultValueToLoad) {
          await onResolve(input.defaultValue as string[]);
        }
      }
      resolveDefaultValue();
    },
    [onResolve, previousDefaultValue, input],
  );

  const onDropFile = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    onResolve(rows);
  };

  const commonProps = {
    input: input,
    onDropFile: onDropFile,
  };

  // Can be both, an auto-completable (async) multi select or a regular one
  return onLoad ? (
    <AsyncInputMultiSelect {...commonProps} {...props} onLoad={onLoad} />
  ) : (
    <InputMultiSelectOld {...commonProps} {...props} />
  );
};

export default MultiSelectWithFileSupport;
