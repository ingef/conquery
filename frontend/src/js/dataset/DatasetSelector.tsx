import { memo, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { DatasetT, SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import InputSelect from "../ui-components/InputSelect/InputSelect";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "../ui-components/Tooltip";

import { useSelectDataset } from "./actions";

const root = tv({
  base: ["flex items-center justify-end", "text-gray-800"],
});

/* the old styles set `font-size: #dadada` (a color) — invalid, so the
   font size was always inherited; only the padding was real */
const headline = tv({ base: "pr-3" });

const useIsDatasetSelectDisabled = () => {
  const isHistoryOpen = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isOpen,
  );
  const isPreviewOpen = useSelector<StateT, boolean>(
    (state) => state.preview.isOpen,
  );

  return useMemo(() => {
    return isHistoryOpen || isPreviewOpen;
  }, [isHistoryOpen, isPreviewOpen]);
};

const DatasetSelector = () => {
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const datasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );
  const error = useSelector<StateT, string | null>(
    (state) => state.datasets.error,
  );

  const selectDataset = useSelectDataset();

  const onChange = useCallback(
    (value: SelectOptionT | null) =>
      exists(value)
        ? selectDataset(value.value as string)
        : selectDataset(null),

    [selectDataset],
  );

  const options = useMemo(
    () => datasets.map((db) => ({ value: db.id, label: db.label })),
    [datasets],
  );

  const selected = useMemo(
    () => options.find((set) => selectedDatasetId === set.value),
    [options, selectedDatasetId],
  );

  const disabled = useIsDatasetSelectDisabled();

  return (
    <DatasetSelectorUI
      options={options}
      selected={selected}
      onChange={onChange}
      error={error}
      disabled={disabled}
    />
  );
};

interface DatasetSelectorUIProps {
  error: string | null;
  selected?: SelectOptionT;
  disabled?: boolean;
  options: SelectOptionT[];
  onChange: (datasetId: SelectOptionT | null) => void;
}

const DatasetSelectorUI = memo(
  ({
    selected,
    onChange,
    error,
    options,
    disabled,
  }: DatasetSelectorUIProps) => {
    const { t } = useTranslation();

    return (
      <TooltipTrigger delay={tooltipDelay.long}>
        <TooltipTarget
          as="div"
          excludeFromTabOrder
          className={root()}
          data-test-id="dataset-selector"
        >
          <span className={headline()}>{t("datasetSelector.label")}</span>
          <InputSelect
            className="min-w-[300px]"
            value={selected || null}
            onChange={onChange}
            placeholder={
              error ? t("datasetSelector.error") : t("inputSelect.placeholder")
            }
            disabled={disabled || exists(error)}
            options={options}
          />
        </TooltipTarget>
        <Tooltip>
          {disabled ? t("datasetSelector.disabled") : t("help.datasetSelector")}
        </Tooltip>
      </TooltipTrigger>
    );
  },
);

export default DatasetSelector;
