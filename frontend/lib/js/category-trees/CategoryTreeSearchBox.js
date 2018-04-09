import { connect } from 'react-redux';
import { SearchBox } from '../form-components';
import { searchTrees } from './actions';

const mapStateToProps = (state) => ({
  query: state.queryStr,
  onSearch: state.onSearch,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (values) => dispatch(searchTrees(values)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
