import { connect } from "react-redux";

import Tags from "../../tags/Tags";

import { addTagToPreviousQueriesSearch } from "../search/actions";

const tagContainsAnySearch = (tag, searches) => {
  return searches.some(
    search => tag.toLowerCase().indexOf(search.toLowerCase()) !== -1
  );
};

type PropsType = {
  tags?: string[];
};

const mapStateToProps = (state, ownProps: PropsType) => ({
  tags: (ownProps.tags || []).map(tag => ({
    label: tag,
    isSelected: tagContainsAnySearch(tag, state.previousQueriesSearch)
  }))
});

const mapDispatchToProps = (dispatch: any) => ({
  onClickTag: tag => dispatch(addTagToPreviousQueriesSearch(tag))
});

export default connect(mapStateToProps, mapDispatchToProps)(Tags);
