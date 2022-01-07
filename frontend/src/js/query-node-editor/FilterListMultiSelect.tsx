import { FC, useState } from "react";

import { usePostFilterValuesResolve } from "../api/api";
import type {
  FilterIdT,
  PostFilterResolveResponseT,
  SelectOptionT,
} from "../api/types";
import { exists } from "../common/helpers/exists";
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

  total?: number;
  onLoad?: (prefix: string, page: number, pageSize: number) => Promise<void>;

  value: SelectOptionT[];
  onChange: (value: SelectOptionT[]) => void;
}

const PAGE_SIZE = 200;

const getPageToLoad = (
  currentOptionsCount: number,
  total?: number,
): number | null => {
  if (currentOptionsCount === 0) return 0;

  const moreCanBeLoaded = exists(total) && currentOptionsCount < total;
  if (moreCanBeLoaded) {
    return Math.floor(currentOptionsCount / PAGE_SIZE) + 1;
  }

  return null;
};

const FilterListMultiSelect: FC<PropsT> = ({
  context,
  value,
  onChange,
  label,
  indexPrefix,
  options,
  disabled,
  allowDropFile,

  total,
  onLoad,
}) => {
  const [resolved, setResolved] = useState<PostFilterResolveResponseT | null>(
    null,
  );
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<boolean>(false);
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const postFilterValuesResolve = usePostFilterValuesResolve();

  const onLoadMore = async (
    prefix: string,
    { shouldReset }: { shouldReset?: boolean } = {},
  ) => {
    if (onLoad && !loading) {
      const pageToLoad = shouldReset ? 0 : getPageToLoad(options.length, total);

      if (pageToLoad === null) return;

      setLoading(true);
      try {
        await onLoad(prefix, pageToLoad, PAGE_SIZE);
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
        total={total}
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
