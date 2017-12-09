import React from 'react';
import T     from 'i18n-react';

import logo  from '../../images/conquery_logo.svg'

class Header extends React.Component {
  render() {
    return (
      <header className="header">
        <img
          src={logo}
          className="header__logo"
        />
        <span className="header__spacer" />
        <h1 className="header__headline">{T.translate('headline')}</h1>
      </header>
    );
  }
}

export default Header;
