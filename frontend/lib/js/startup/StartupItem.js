// @flow

import React                from 'react';


type PropsType = {
  history: Object,
  match: Object,
  location: Object,
  onStartup: Function,
};

class StartupItem extends React.Component {
  props: PropsType;

  componentDidMount() {
    // Ignore location changes that were triggered by the application
    if (this.props.history.action !== 'REPLACE') {
      const { match } = this.props;
      const params = match && match.params ? match.params : {};

      this.props.onStartup(params);
    }
  }

  render() { return null; }
}

export default StartupItem;
