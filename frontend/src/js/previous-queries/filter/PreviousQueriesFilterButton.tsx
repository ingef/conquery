import React from "react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import classnames from "classnames";

import { setPreviousQueriesFilter } from "./actions";

type PropsType = {
  value: string;
  text: string;
  selectedFilter: string;
  setFilter: Function;
};

const PreviousQueriesFilterButton = (props: PropsType) => {
  return (
    <button
      className={classnames(
        "previous-queries-filter-button",
        `previous-queries-filter-button--${props.value}`,
        {
          "previous-queries-filter-button--selected":
            props.selectedFilter === props.value
        }
      )}
      onClick={() => props.setFilter(props.value)}
    >
      {props.text}
    </button>
  );
};

const mapStateToProps = state => ({
  selectedFilter: state.previousQueriesFilter
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  setFilter: filter => dispatch(setPreviousQueriesFilter(filter))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PreviousQueriesFilterButton);
