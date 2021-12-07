import { FC, useState } from "react";

import { usePostFilterValuesResolve } from "../api/api";
import type {
  FilterIdT,
  PostFilterResolveResponseT,
  SelectOptionT,
} from "../api/types";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

import type { FiltersContextT } from "./TableFilter";
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

  onLoad?: (prefix: string) => Promise<void>;

  value: SelectOptionT[];
  onChange: (value: SelectOptionT[]) => void;
}

const FilterListMultiSelect: FC<PropsT> = ({
  context,
  value,
  onChange,
  label,
  indexPrefix,
  options,
  disabled,
  allowDropFile,

  onLoad,
}) => {
  const [resolved, setResolved] = useState<PostFilterResolveResponseT | null>(
    null,
  );
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<boolean>(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const postFilterValuesResolve = usePostFilterValuesResolve();

  const onLoadMore = async (prefix: string) => {
    if (onLoad && !loading) {
      setLoading(true);
      try {
        await onLoad(prefix);
      } catch (e) {
        // fail silently
        console.error(e);
      }
      setLoading(false);
    }
  };

  const onDropFile = async (rows: string[]) => {
    setLoading(true);

    try {
      const r = await postFilterValuesResolve(
        context.datasetId,
        context.treeId,
        context.tableId,
        context.filterId,
        rows,
      );

      setResolved(r);
      setIsModalOpen(!!r.unknownCodes && r.unknownCodes.length > 0);

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
        onChange={onChange}
        label={label}
        options={options}
        loading={loading}
        disabled={disabled}
        indexPrefix={indexPrefix}
        onLoadMore={onLoad ? onLoadMore : undefined}
        onResolve={allowDropFile ? onDropFile : undefined}
      />
    </>
  );
};

export default FilterListMultiSelect;
