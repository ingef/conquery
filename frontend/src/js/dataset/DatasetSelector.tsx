import styled from "@emotion/styled";
import { FC, memo, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetT, SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import WithTooltip from "../tooltip/WithTooltip";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { useSelectDataset } from "./actions";

const Root = styled("div")`
  color: ${({ theme }) => theme.col.black};
  display: flex;
  align-items: center;
  justify-content: flex-end;
`;

const Headline = styled("span")`
  font-size: ${({ theme }) => theme.col.grayLight};
  padding-right: 12px;
`;

const SxInputSelect = styled(InputSelect)`
  min-width: 300px;
`;

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

const DatasetSelector: FC = () => {
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
      <WithTooltip
        text={
          disabled ? t("datasetSelector.disabled") : t("help.datasetSelector")
        }
        lazy
      >
        <Root data-test-id="dataset-selector">
          <Headline>{t("datasetSelector.label")}</Headline>
          <SxInputSelect
            value={selected || null}
            onChange={onChange}
            placeholder={
              error ? t("datasetSelector.error") : t("inputSelect.placeholder")
            }
            disabled={disabled || exists(error)}
            options={options}
          />
        </Root>
      </WithTooltip>
    );
  },
);

export default DatasetSelector;
