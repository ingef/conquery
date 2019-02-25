// @flow

import React           from 'react';
import T               from 'i18n-react';
import { connect }     from 'react-redux';

type PropsType = {
  version: string,
  isDevelopment: boolean,
};

class Header extends React.Component<PropsType> {
  render() {
    return (
      <header className="header">
        <div
          className="header__logo"
          title={'Conquery ' + this.props.version}
        />
        <span className="header__spacer" />
        <h1 className="header__headline">{T.translate('headline')}</h1>
        {
          this.props.isDevelopment &&
          <h1 title={'Conquery ' + this.props.version} className="header__version">
            {this.props.version}
          </h1>
        }
      </header>
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  return {
    version: state.startup.config.version || '',
    isDevelopment: !state.startup.config.production || false,
  }
};

export default connect(mapStateToProps, {})(Header);
