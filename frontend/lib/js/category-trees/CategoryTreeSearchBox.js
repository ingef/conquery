// @flow

import { connect }          from 'react-redux';
import { SearchBox }        from '../form-components';
import { searchTreesStart } from './actions';

const mapStateToProps = (state) => ({
  qry: state.categoryTrees.search.query,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (query) => dispatch(searchTreesStart(query)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
