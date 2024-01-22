import { FC, useState } from "react";

import { usePostFilterValuesResolve } from "../api/api";
import type {
  FilterT,
  PostFilterResolveResponseT,
  PostFilterSuggestionsResponseT,
  SelectOptionT,
} from "../api/types";
import { exists } from "../common/helpers/exists";
import InputMultiSelect from "../ui-components/InputMultiSelect/InputMultiSelect";

import UploadFilterListModal from "./UploadFilterListModal";
import { filterSuggestionToSelectOption } from "./suggestionsHelper";

const PAGE_SIZE = 25;

const getPageToLoad = (
  prevPageLoaded: number | null,
  currentOptionsCount: number,
  total?: number,
): number | null => {
  if (currentOptionsCount === 0) return 0;

  const moreCanBeLoaded = exists(total) && currentOptionsCount < total;
  if (!moreCanBeLoaded) {
    return null;
  }

  const nextPageBasedOnLoadedCount = Math.max(
    0,
    Math.ceil(currentOptionsCount / PAGE_SIZE),
  );

  return prevPageLoaded === null
    ? nextPageBasedOnLoadedCount
    : prevPageLoaded + 1;
};

interface PropsT {
  filterId: FilterT["id"];

  label: string;
  indexPrefix?: number;
  options: SelectOptionT[];
  disabled?: boolean;
  tooltip?: string;
  allowDropFile?: boolean;
  creatable?: boolean;

  total?: number;
  onLoad?: (
    prefix: string,
    page: number,
    pageSize: number,
    config?: { returnOnly?: boolean },
  ) => Promise<PostFilterSuggestionsResponseT | null>;

  value: SelectOptionT[];
  onChange: (value: SelectOptionT[]) => void;
}

const FilterListMultiSelect: FC<PropsT> = ({
  filterId,
  value,
  onChange,
  label,
  tooltip,
  indexPrefix,
  options,
  disabled,
  allowDropFile,
  creatable,

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
  const [prevPageLoaded, setPrevPageLoaded] = useState<number | null>(null);

  const onLoadMore = async (
    prefix: string,
    { shouldReset }: { shouldReset?: boolean } = {},
  ) => {
    if (!onLoad || loading) return;

    const pageToLoad = shouldReset
      ? 0
      : getPageToLoad(prevPageLoaded, options.length, total);

    if (pageToLoad === null) return;

    setLoading(true);
    try {
      await onLoad(prefix, pageToLoad, PAGE_SIZE);
      setPrevPageLoaded(pageToLoad);
    } catch (e) {
      // fail silently
      console.error(e);
    }
    setLoading(false);
  };

  const onLoadAndInsertAll = async (prefix: string) => {
    if (!onLoad || loading || !exists(total)) return;

    setLoading(true);
    try {
      const suggestions = await onLoad(prefix, 0, total, { returnOnly: true });
      const options = suggestions?.values.map(filterSuggestionToSelectOption);

      if (options) {
        onChange(options);
      }
    } catch (e) {
      // fail silently
      console.error(e);
    }
    setLoading(false);
  };

  const onDropFile = async (rows: string[]) => {
    setLoading(true);

    try {
      const r = await postFilterValuesResolve(filterId, rows);
      const hasUnknownCodes = r.unknownCodes && r.unknownCodes.length > 0;

      if (hasUnknownCodes) {
        setResolved(r);
        setIsModalOpen(true);
      } else {
        onSubmitResolvedValues(r, { includeUnresolved: false });
      }
    } catch (e) {
      setError(true);
    }

    setLoading(false);
  };

  const onSubmitResolvedValues = (
    resolvedResponse: PostFilterResolveResponseT,
    {
      includeUnresolved,
    }: {
      includeUnresolved?: boolean;
    },
  ) => {
    const hasSomethingToInsert =
      (resolvedResponse.resolvedFilter?.value?.length || 0) > 0 ||
      resolvedResponse.unknownCodes.length > 0;

    console.log("hasSomethingToInsert", hasSomethingToInsert);
    console.log("resolvedResponse", resolvedResponse);

    if (!hasSomethingToInsert) return;

    const value = includeUnresolved
      ? [
          ...resolvedResponse.resolvedFilter!.value,
          ...resolvedResponse.unknownCodes.map((item) => ({
            label: item,
            value: item,
          })),
        ]
      : resolvedResponse.resolvedFilter!.value;

    onChange(value);
  };

  return (
    <>
      {allowDropFile && isModalOpen && resolved && (
        <UploadFilterListModal
          resolved={resolved}
          loading={loading}
          error={error}
          onSubmit={onSubmitResolvedValues}
          onClose={() => setIsModalOpen(false)}
        />
      )}
      <InputMultiSelect
        value={value}
        onChange={onChange}
        label={label}
        tooltip={tooltip}
        options={options}
        total={total}
        loading={loading}
        disabled={disabled}
        indexPrefix={indexPrefix}
        creatable={creatable}
        onLoadMore={onLoad ? onLoadMore : undefined}
        onLoadAndInsertAll={onLoad ? onLoadAndInsertAll : undefined}
        onResolve={allowDropFile ? onDropFile : undefined}
      />
    </>
  );
};

export default FilterListMultiSelect;
