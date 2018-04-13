// @flow

import { connect }          from 'react-redux';
import { SearchBox }        from '../form-components';
import { searchTrees } from './actions';

const mapStateToProps = (state) => ({
  qry: state.categoryTrees.search.query,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (query) => dispatch(searchTrees(query)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
