// @flow

import { connect }          from 'react-redux';
import { SearchBox }        from '../form-components';
import { searchTrees }      from './actions';

const mapStateToProps = (state) => ({
  searchResult: state.categoryTrees.search,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (query) => dispatch(searchTrees(query)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
