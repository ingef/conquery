// @flow

import React, { useEffect } from "react";
import { connect } from "react-redux";

import { startup, loadConfig } from "./actions";

type PropsType = {
  startup: Function
};

const Startup = (props: PropsType) => {
  useEffect(() => {
    props.startup();
  });

  return <></>;
};

const mapStateToProps = state => ({});

const mapDispatchToProps = dispatch => {
  return {
    startup: () => {
      dispatch(loadConfig());
      dispatch(startup());
    }
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Startup);
