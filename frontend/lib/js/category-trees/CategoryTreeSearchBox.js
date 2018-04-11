import { connect } from 'react-redux';
import { SearchBox } from '../form-components';
import { searchTreesStart } from './actions';

const mapStateToProps = (state) => ({
  searchStr: state.categoryTrees.search.searchStr,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (values) => dispatch(searchTreesStart(values)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
