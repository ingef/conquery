// @flow

import React                 from 'react';
import type { Dispatch }     from 'redux-thunk';
import T                     from 'i18n-react';
import Select                from 'react-select';
import { connect }           from 'react-redux';

import { isEmpty }           from '../common/helpers';
import type { DatasetType }  from './reducer';

import * as actions          from './actions';

type PropsType = {
  selectedDatasetId: number,
  datasets: DatasetType[],
  error: string,
  selectDataset: Function,
};

const DatasetSelector = (props: PropsType) => (
  <div className="dataset-selector">
    <Select
      name="dataset-selector"
      value={props.error
        ? -1
        : props.selectedDatasetId
      }
      onChange={(value) =>
        !isEmpty(value)
          ? props.selectDataset(value.value)
          : props.selectDataset(null)
      }
      placeholder={T.translate('reactSelect.placeholder')}
      autosize
      clearable={false}
      searchable={false}
      disabled={!!props.error}
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
  selectedDatasetId: state.datasets.selectedDatasetId,
  datasets: state.datasets.data,
  error: state.datasets.error,
  query: state.query,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  selectDataset: (datasets, datasetId, query) =>
    dispatch(actions.selectDataset(datasets, datasetId, query)),
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...ownProps,
  ...stateProps,
  ...dispatchProps,
  selectDataset: (datasetId) =>
    dispatchProps.selectDataset(
      stateProps.datasets,
      datasetId,
      stateProps.query
    )
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(DatasetSelector);
