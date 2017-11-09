// @flow

import React from 'react';
import { storeAuthToken }    from './helper';

const WithAuthToken = (Component: any) => {
  type PropsType = {
    location: {
      query: Object
    }
  };

  class AuthToken extends React.Component {
    props: PropsType;

    componentWillMount() {
      const { query } = this.props.location;

      if (query.access_token)
        storeAuthToken(query.access_token);
    }

    render() {
      return <Component />
    }
  }

  return AuthToken;
};

export default WithAuthToken;
