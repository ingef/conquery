// @flow

import { connect }          from 'react-redux';
import { SearchBox }        from '../form-components';
import { searchTrees }      from './actions';

const mapStateToProps = (state) => ({
  searchResult: state.categoryTrees.search,
  onSearch: state.onSearch,
  searchConfig: state.startup.config.search ? state.startup.config.search : null
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (datasetId, query, limit) => dispatch(searchTrees(datasetId, query, limit)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
