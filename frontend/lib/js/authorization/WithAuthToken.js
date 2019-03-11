// @flow

import React from "react";
import queryString from "query-string";
import { storeAuthToken } from "./helper";

const WithAuthToken = (Component: any) => {
  type PropsType = {
    location: {
      search: Object
    }
  };

  class AuthToken extends React.Component {
    props: PropsType;

    componentWillMount() {
      const { search } = this.props.location;
      const accessToken = search
        ? queryString.parse(search).access_token
        : null;

      if (accessToken) storeAuthToken(accessToken);
    }

    render() {
      return <Component />;
    }
  }

  return AuthToken;
};

export default WithAuthToken;
