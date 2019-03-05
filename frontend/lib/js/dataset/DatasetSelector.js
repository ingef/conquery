// @flow

import React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import T from "i18n-react";
import { connect } from "react-redux";

import { isEmpty } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";

import type { DatasetType } from "./reducer";
import { selectDataset } from "./actions";

const Root = styled("div")`
  min-width: 200px;
  padding: 0px 10px 0 20px;
`;

type PropsType = {
  selectedDatasetId: String,
  datasets: DatasetType[],
  error: string,
  loadDatasets: Function,
  selectDataset: Function
};

const DatasetSelector = (props: PropsType) => {
  const { selectedDatasetId, datasets, selectDataset, error } = props;

  const options =
    datasets && datasets.map(db => ({ value: db.id, label: db.label }));
  const selected = options.filter(set => selectedDatasetId === set.value);

  return (
    <Root>
      <ReactSelect
        name="dataset-selector"
        value={error ? -1 : selected}
        onChange={value =>
          !isEmpty(value)
            ? selectDataset(value.value, selectedDatasetId)
            : selectDataset(null, selectedDatasetId)
        }
        placeholder={T.translate("reactSelect.placeholder")}
        isDisabled={!!error}
        options={
          error
            ? [
                {
                  value: -1,
                  label: T.translate("datasetSelector.error")
                }
              ]
            : options
        }
      />
    </Root>
  );
};

const mapStateToProps = state => ({
  selectedDatasetId: state.datasets.selectedDatasetId,
  datasets: state.datasets.data,
  error: state.datasets.error,
  query: state.panes.right.tabs.queryEditor.query
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  selectDataset: (datasets, datasetId, previouslySelectedDatasetId, query) =>
    dispatch(
      selectDataset(datasets, datasetId, previouslySelectedDatasetId, query)
    )
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  selectDataset: (datasetId, selectedDatasetId) =>
    dispatchProps.selectDataset(
      stateProps.datasets,
      datasetId,
      selectedDatasetId,
      stateProps.query
    )
});

export default connect(
  mapStateToProps,
  mapDispatchToProps,
  mergeProps
)(DatasetSelector);
