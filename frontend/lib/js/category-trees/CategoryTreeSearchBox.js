// @flow

import { connect } from "react-redux";
import { SearchBox } from "../form-components";
import { searchTrees, changeSearchQuery, clearSearchQuery } from "./actions";

const mapStateToProps = state => ({
  searchResult: state.categoryTrees.search
});

const mapDispatchToProps = dispatch => ({
  onSearch: (datasetId, query) => dispatch(searchTrees(datasetId, query)),
  onChange: query => dispatch(changeSearchQuery(query)),
  onClearQuery: () => dispatch(clearSearchQuery())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SearchBox);
