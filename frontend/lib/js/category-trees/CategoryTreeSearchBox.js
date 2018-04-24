// @flow

import { connect }          from 'react-redux';
import { SearchBox }        from '../form-components';
import { searchTrees }      from './actions';

const mapStateToProps = (state) => ({
  searchResult: state.categoryTrees.search,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (datasetId, query) => dispatch(searchTrees(datasetId, query)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
