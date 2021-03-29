import React, { FC } from "react";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";
import { StateT } from "app-types";
import { useTranslation } from "react-i18next";

import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import { isEmpty } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";

import { DatasetT } from "./reducer";
import { useSelectDataset } from "./actions";

const Root = styled("div")`
  min-width: 300px;
  padding: 0px 0 0 20px;
  color: ${({ theme }) => theme.col.black};
`;

const DatasetSelector: FC = () => {
  const { t } = useTranslation();
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const datasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data
  );
  const error = useSelector<StateT, string | null>(
    (state) => state.datasets.error
  );
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query
  );

  const selectDataset = useSelectDataset();

  const onSelectDataset = (datasetId: string | null) =>
    selectDataset(datasets, datasetId, selectedDatasetId, query);

  const options =
    datasets && datasets.map((db) => ({ value: db.id, label: db.label }));
  const selected = options.filter((set) => selectedDatasetId === set.value);

  return (
    <Root>
      <ReactSelect
        name="dataset-selector"
        value={error ? -1 : selected}
        onChange={(value) =>
          !isEmpty(value) ? onSelectDataset(value.value) : onSelectDataset(null)
        }
        placeholder={
          error ? t("datasetSelector.error") : t("reactSelect.placeholder")
        }
        isDisabled={!!error}
        options={options}
      />
    </Root>
  );
};

export default DatasetSelector;
