import React, { FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { useSelector } from "react-redux";

import { isEmpty } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";

import { DatasetT } from "./reducer";
import { useSelectDataset } from "./actions";
import { StateT } from "app-types";
import { StandardQueryType } from "../standard-query-editor/types";

const Root = styled("div")`
  min-width: 300px;
  padding: 0px 0 0 20px;
  color: ${({ theme }) => theme.col.black};
`;

const DatasetSelector: FC = () => {
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const datasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data
  );
  const error = useSelector<StateT, string | null>(
    (state) => state.datasets.error
  );
  const query = useSelector<StateT, StandardQueryType>(
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
          error
            ? T.translate("datasetSelector.error")
            : T.translate("reactSelect.placeholder")
        }
        isDisabled={!!error}
        options={options}
      />
    </Root>
  );
};

export default DatasetSelector;
