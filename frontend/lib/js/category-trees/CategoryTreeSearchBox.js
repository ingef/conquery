// @flow

import { connect }    from 'react-redux';
import { SearchBox }  from '../form-components';
import {
  searchTrees,
  changeSearchQuery,
  clearSearchQuery
}                     from './actions';

const mapStateToProps = (state) => ({
  searchResult: state.categoryTrees.search,
  onSearch: state.onSearch,
  searchConfig: state.startup.config.search ? state.startup.config.search : {}
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (datasetId, query, limit) => dispatch(searchTrees(datasetId, query, limit)),
  onChange: (query) => dispatch(changeSearchQuery(query)),
  onClearQuery: () => dispatch(clearSearchQuery()),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
