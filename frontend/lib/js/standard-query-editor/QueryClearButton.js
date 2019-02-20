// @flow

import React from "react";
import { connect } from "react-redux";
import T from "i18n-react";

import IconButton from "../button/IconButton";

import { clearQuery } from "./actions";

type PropsType = {
  clearQuery: () => void,
  isVisible: boolean
};

const QueryClearButton = (props: PropsType) => {
  return (
    props.isVisible && (
      <div className="query-clear-button">
        <IconButton frame onClick={props.clearQuery} icon="trash-o">
          {T.translate("common.clear")}
        </IconButton>
      </div>
    )
  );
};

const mapStateToProps = state => ({
  isVisible: state.panes.right.tabs.queryEditor.query.length !== 0
});

const mapDispatchToProps = dispatch => ({
  clearQuery: () => dispatch(clearQuery())
});

const ConnectedQueryClearButton = connect(
  mapStateToProps,
  mapDispatchToProps
)(QueryClearButton);
export { ConnectedQueryClearButton as QueryClearButton };
