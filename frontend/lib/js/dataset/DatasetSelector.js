// @flow

import React                 from 'react';
import type { Dispatch }     from 'redux-thunk';
import T                     from 'i18n-react';
import Select                from 'react-select';
import { connect }           from 'react-redux';

import { isEmpty }           from '../common/helpers';

import type { DatasetType }  from './reducer';
import { selectDataset }     from './actions';

type PropsType = {
  selectedDatasetId: String,
  datasets: DatasetType[],
  error: string,
  loadDatasets: Function,
  selectDataset: Function,
};

const DatasetSelector = (props: PropsType) => (
  <div className="dataset-selector">
    <Select
      name="dataset-selector"
      value={props.error
        ? -1
        : { value: props.selectedDatasetId, label: props.selectedDatasetId }
      }
      onChange={(value) =>
        !isEmpty(value)
          ? props.selectDataset(value.value, props.selectedDatasetId)
          : props.selectDataset(null, props.selectedDatasetId)
      }
      placeholder={T.translate('reactSelect.placeholder')}
      isDisabled={!!props.error}
      options={props.error
        ? [{
            value: -1,
            label: T.translate('datasetSelector.error')
          }]
        : props.datasets.map(db => ({
            value: db.id,
            label: db.label
          }))
      }
    />
  </div>
);

const mapStateToProps = (state) => ({
  datasets: state.datasets.data,
  error: state.datasets.error,
  query: state.panes.right.tabs.queryEditor.query,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  selectDataset: (datasets, datasetId, previouslySelectedDatasetId, query) =>
    dispatch(selectDataset(datasets, datasetId, previouslySelectedDatasetId, query)),
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

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(DatasetSelector);
