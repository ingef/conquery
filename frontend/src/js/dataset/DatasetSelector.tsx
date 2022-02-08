import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { exists } from "../common/helpers/exists";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import WithTooltip from "../tooltip/WithTooltip";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { useSelectDataset } from "./actions";
import { DatasetT } from "./reducer";

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

const DatasetSelector: FC = () => {
  const { t } = useTranslation();
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const datasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );
  const error = useSelector<StateT, string | null>(
    (state) => state.datasets.error,
  );
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );

  const selectDataset = useSelectDataset();

  const onSelectDataset = (datasetId: string | null) =>
    selectDataset(datasets, datasetId, selectedDatasetId, query);

  const options =
    datasets && datasets.map((db) => ({ value: db.id, label: db.label }));
  const selected = options.find((set) => selectedDatasetId === set.value);

  return (
    <WithTooltip text={t("help.datasetSelector")} lazy>
      <Root data-test-id="dataset-selector" >
        <Headline>{t("datasetSelector.label")}</Headline>
        <SxInputSelect
          value={selected || null}
          onChange={(value) =>
            exists(value)
              ? onSelectDataset(value.value as string)
              : onSelectDataset(null)
          }
          placeholder={
            error ? t("datasetSelector.error") : t("inputSelect.placeholder")
          }
          disabled={exists(error)}
          options={options}
        />
      </Root>
    </WithTooltip>
  );
};

export default DatasetSelector;
