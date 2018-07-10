import React           from 'react';
import PropTypes       from 'prop-types';
import T               from 'i18n-react';
import { connect }     from 'react-redux';

class Header extends React.Component {
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

Header.propTypes = {
  version: PropTypes.string,
  isDevelopment: PropTypes.bool
};

const mapStateToProps = (state, ownProps) => {
  return {
    version: state.config ? state.config.version : '',
    isDevelopment: state.config ? !state.config.production : false,
  }
};

export default connect(mapStateToProps, {})(Header);
