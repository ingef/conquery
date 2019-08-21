// @flow

import * as React from "react";
import type { FieldPropsType } from "redux-form";

import type { SelectOptionsT, FilterIdT } from "../api/types";

import AsyncInputMultiSelect from "../form-components/AsyncInputMultiSelect";
import { getUniqueFileRows } from "../common/helpers/fileHelper";

import { postFilterValuesResolve } from "../api/api";

import type { FilterContextT } from "./TableFilters";
import UploadFilterListModal from "./UploadFilterListModal";

type BigMultiSelectContextT = FilterContextT & {
  filterId: FilterIdT
};

type PropsT = FieldPropsType & {
  context: BigMultiSelectContextT,

  label: string,
  isLoading: boolean,
  options: SelectOptionsT,
  disabled?: ?boolean,
  startLoadingThreshold: number,
  tooltip?: string,
  onLoad: Function,
  onDropFile: Function,
  allowDropFile?: ?boolean
};

export default ({
  context,
  label,
  options,
  disabled,
  tooltip,
  startLoadingThreshold,
  onLoad,
  isLoading,
  input,
  allowDropFile
}: PropsT) => {
  const [resolved, setResolved] = React.useState(null);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState(false);
  const [isModalOpen, setIsModalOpen] = React.useState(false);

  const onDropFile = async file => {
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
      <AsyncInputMultiSelect
        input={input}
        label={label}
        options={options}
        isLoading={isLoading}
        startLoadingThreshold={startLoadingThreshold}
        disabled={disabled}
        onLoad={onLoad}
        onDropFile={onDropFile}
        allowDropFile={allowDropFile}
      />
    </>
  );
};
