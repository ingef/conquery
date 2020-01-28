// @flow

import React from "react";
import { storeAuthToken, getStoredAuthToken } from "./helper";
import { connect } from "react-redux";
import { push } from "react-router-redux";
import { isLoginDisabled } from "../environment";

const WithAuthToken = (Component: any) => {
  type PropsType = {
    goToLogin: () => void,
    location: {
      search: Object
    }
  };

  class AuthToken extends React.Component {
    props: PropsType;

    componentWillMount() {
      const { search } = this.props.location;

      var params = new URLSearchParams(search);

      const accessToken = params.get("access_token");

      if (accessToken) storeAuthToken(accessToken);

      if (!isLoginDisabled()) {
        const authToken = getStoredAuthToken();

        if (!authToken) {
          this.props.goToLogin();

          return null;
        }
      }
    }

    render() {
      const { location, ...rest } = this.props;

      return <Component {...rest} />;
    }
  }

  return connect(
    () => ({}),
    dispatch => ({
      goToLogin: () => dispatch(push("/login"))
    })
  )(AuthToken);
};

export default WithAuthToken;
