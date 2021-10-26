import { FC, useState } from "react";

import { usePostFilterValuesResolve } from "../api/api";
import type {
  FilterIdT,
  FilterSuggestion,
  PostFilterResolveResponseT,
  SelectOptionT,
} from "../api/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

import type { FiltersContextT } from "./TableFilters";
import UploadFilterListModal from "./UploadFilterListModal";

interface FilterContextT extends FiltersContextT {
  filterId: FilterIdT;
}

interface PropsT {
  context: FilterContextT;

  label: string;
  indexPrefix?: number;
  options: SelectOptionT[];
  disabled?: boolean;
  tooltip?: string;
  allowDropFile?: boolean;

  isLoading?: boolean;
  onLoad?: (prefix: string) => void;

  defaultValue?: string[];
  value: SelectOptionT[] | FilterSuggestion[];
  onChange: (value: SelectOptionT[] | FilterSuggestion[] | null) => void;
}

const FilterListMultiSelect: FC<PropsT> = ({
  context,
  value,
  defaultValue,
  onChange,
  label,
  indexPrefix,
  options,
  disabled,
  allowDropFile,

  onLoad,
  isLoading,
}) => {
  const [resolved, setResolved] = useState<PostFilterResolveResponseT | null>(
    null,
  );
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<boolean>(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const postFilterValuesResolve = usePostFilterValuesResolve();

  const onResolve = async (
    csvLines: string[],
    options: { showModal?: boolean } = {},
  ) => {
    setLoading(true);

    try {
      const r = await postFilterValuesResolve(
        context.datasetId,
        context.treeId,
        context.tableId,
        context.filterId,
        csvLines,
      );

      if (options.showModal) {
        setResolved(r);
        setIsModalOpen(!!r.unknownCodes && r.unknownCodes.length > 0);
      }

      if (
        r.resolvedFilter &&
        r.resolvedFilter.value &&
        r.resolvedFilter.value.length > 0
      ) {
        onChange(r.resolvedFilter.value);
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
      <InputMultiSelect
        value={value}
        defaultValue={
          defaultValue?.map((s) => ({ value: s, label: s })) || undefined
        }
        onChange={onChange}
        label={label}
        options={options}
        loading={isLoading || loading}
        disabled={disabled}
        indexPrefix={indexPrefix}
        onLoadMore={onLoad}
        onResolve={allowDropFile ? onResolve : undefined}
      />
    </>
  );
};

export default FilterListMultiSelect;
