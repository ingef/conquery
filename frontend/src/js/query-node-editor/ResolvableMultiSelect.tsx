import React, { FC } from "react";

import type { FilterIdT, SelectOptionT } from "../api/types";

import AsyncInputMultiSelect from "../form-components/AsyncInputMultiSelect";
import InputMultiSelect, {
  MultiSelectInputProps,
} from "../form-components/InputMultiSelect";
import { getUniqueFileRows } from "../common/helpers/fileHelper";

import { postFilterValuesResolve } from "../api/api";

import type { FiltersContextT } from "./TableFilters";
import UploadFilterListModal from "./UploadFilterListModal";

interface FilterContextT extends FiltersContextT {
  filterId: FilterIdT;
}

interface PropsT {
  context: FilterContextT;

  label: string;
  options: SelectOptionT[];
  disabled?: boolean;
  tooltip?: string;
  allowDropFile?: boolean;

  isLoading?: boolean;
  onLoad?: Function;
  startLoadingThreshold: number;

  input: MultiSelectInputProps;
}

const ResolvableMultiSelect: FC<PropsT> = ({
  context,
  input,
  label,
  options,
  disabled,
  tooltip,
  allowDropFile,

  startLoadingThreshold,
  onLoad,
  isLoading,
}) => {
  const [resolved, setResolved] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState(false);
  const [isModalOpen, setIsModalOpen] = React.useState(false);

  // Can be both, an auto-completable (async) multi select or a regular one
  const Component = !!onLoad ? AsyncInputMultiSelect : InputMultiSelect;

  const onDropFile = async (file) => {
    setLoading(true);

    const rows = await getUniqueFileRows(file);

    try {
      const r = await postFilterValuesResolve(
        context.datasetId,
        context.treeId,
        context.tableId,
        context.filterId,
        rows
      );

      setResolved(r);
      setIsModalOpen(r.unknownCodes && r.unknownCodes.length > 0);

      if (
        r.resolvedFilter &&
        r.resolvedFilter.value &&
        r.resolvedFilter.value.length > 0
      ) {
        input.onChange(r.resolvedFilter.value);
      }
    } catch (e) {
      setError(true);
    }

    setLoading(false);
  };

  return (
    <>
      {allowDropFile && isModalOpen && (
        <UploadFilterListModal
          resolved={resolved}
          loading={loading}
          error={error}
          onClose={() => setIsModalOpen(false)}
        />
      )}
      <Component
        input={input}
        label={label}
        options={options}
        isLoading={isLoading || loading}
        startLoadingThreshold={startLoadingThreshold}
        disabled={disabled}
        onLoad={onLoad}
        onDropFile={onDropFile}
        allowDropFile={allowDropFile}
      />
    </>
  );
};

export default ResolvableMultiSelect;
