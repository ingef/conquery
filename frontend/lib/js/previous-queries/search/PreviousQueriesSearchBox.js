import { connect }                     from 'react-redux';

import { SearchBox }                   from '../../form-components';
import { updatePreviousQueriesSearch } from './actions';

const mapStateToProps = (state) => ({
  search: state.previousQueriesSearch,
  options: state.previousQueries.names,
});

const mapDispatchToProps = (dispatch) => ({
  onSearch: (values) => dispatch(updatePreviousQueriesSearch(values)),
});

export default connect(mapStateToProps, mapDispatchToProps)(SearchBox);
