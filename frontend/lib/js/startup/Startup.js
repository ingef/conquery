// @flow

import React, { useEffect } from "react";
import { connect } from "react-redux";

import { startupOnDataset, loadConfig } from "./actions";

type PropsType = {
  startup: Function
};

const Startup = (props: PropsType) => {
  useEffect(() => {
    props.startup();
  });

  return <></>;
  // <Route
  //   exact
  //   path={templates.toQuery}
  //   render={routeProps => (
  //     <StartupItem
  //       {...routeProps}
  //       onStartup={({ datasetId, queryId }) =>
  //         props.startupOnQuery(datasetId, queryId)
  //       }
  //     />
  //   )}
  // />
};

const mapStateToProps = state => ({});

const mapDispatchToProps = dispatch => {
  return {
    startup: () => {
      dispatch(loadConfig());
      dispatch(startupOnDataset());
    }
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(Startup);
