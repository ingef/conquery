import React from 'react';
import T     from 'i18n-react';

class Header extends React.Component {
  render() {
    return (
      <header className="header">
        <div
          className="header__logo"
        />
        <span className="header__spacer" />
        <h1 className="header__headline">{T.translate('headline')}</h1>
      </header>
    );
  }
}

export default Header;
