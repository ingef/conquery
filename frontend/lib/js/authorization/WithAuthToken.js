// @flow

import React from "react";
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

      var params = new URLSearchParams(search);

      const accessToken = params.get("access_token");

      if (accessToken) storeAuthToken(accessToken);
    }

    render() {
      const { location, ...rest } = this.props;

      return <Component {...rest} />;
    }
  }

  return AuthToken;
};

export default WithAuthToken;
