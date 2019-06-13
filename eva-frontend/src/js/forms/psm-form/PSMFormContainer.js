// @flow

import { connect }        from 'react-redux';

import PSMForm            from './PSMForm';

function mapStateToProps(state) {
  return {
    availableDatasets: state.datasets.data.map(dataset => ({
      label: dataset.label,
      value: dataset.id
    }))
  };
}

export default connect(mapStateToProps)(PSMForm);
