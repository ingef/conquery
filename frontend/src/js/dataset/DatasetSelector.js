// @flow

import React                 from 'react';
import type { Dispatch }     from 'redux-thunk';
import T                     from 'i18n-react';
import Select                from 'react-select';
import { connect }           from 'react-redux';

import { isEmpty }           from '../common/helpers';
import type {
  DatasetType,
  DatasetIdType
}                            from './reducer';

import {
  loadDatasets,
  selectDataset,
}                            from './actions';

type PropsType = {
  selectedDatasetId: String,
  datasetsLoading: Boolean,
  datasets: DatasetType[],
  error: string,
  loadDatasets: Function,
  selectDataset: Function,
};

class DatasetSelector extends React.Component {
  props: PropsType;

  componentDidMount() {
    this.props.loadDatasets(this.props.selectedDatasetId);
  }

  render() {
    return (
      <div className="dataset-selector">
        <Select
          name="dataset-selector"
          value={this.props.error
            ? -1
            : this.props.selectedDatasetId
          }
          onChange={(value) =>
            !isEmpty(value)
              ? this.props.selectDataset(value.value, this.props.selectedDatasetId)
              : this.props.selectDataset(null, this.props.selectedDatasetId)
          }
          placeholder={T.translate('reactSelect.placeholder')}
          autosize
          clearable={false}
          searchable={false}
          disabled={!!this.props.error}
          options={this.props.error
            ? [{
                value: -1,
                label: T.translate('datasetSelector.error')
              }]
            : this.props.datasets.map(db => ({
                value: db.id,
                label: db.label
              }))
          }
        />
      </div>
    )
  }
}


const mapStateToProps = (state) => ({
  datasetsLoading: state.datasets.loading,
  datasets: state.datasets.data,
  error: state.datasets.error,
  query: state.query,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  loadDatasets: (selectedDatasetId) => dispatch(loadDatasets(selectedDatasetId)),
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
    ),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(DatasetSelector);
