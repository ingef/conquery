import React        from 'react';
import PropTypes    from 'prop-types';
import T            from 'i18n-react';
import { connect }  from "react-redux";

class Header extends React.Component {
  render() {
    return (
      <header className="header">
        <div
          className="header__logo"
        />
        <span className="header__spacer" />
        <h1 className="header__headline">{T.translate('headline')}</h1>
        {this.props.development && <h1 className="header__version">{this.props.version}</h1>}
      </header>
    );
  }
}

Header.propTypes = {
  version: PropTypes.string,
  development: PropTypes.bool
};

const mapStateToProps = (state, ownProps) => ({
  version: '1.6-RC4',
  development: true,
});

export default connect(mapStateToProps)(Header);
