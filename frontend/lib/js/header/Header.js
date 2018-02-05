import React           from 'react';
import PropTypes       from 'prop-types';
import T               from 'i18n-react';
import { connect }     from 'react-redux';
import { loadVersion } from './actions';

class Header extends React.Component {
  componentDidMount() {
    this.props.loadVersion();
  }

  render() {
    return (
      <header className="header">
        <div
          className="header__logo"
          title={'Conquery ' + this.props.version}
        />
        <span className="header__spacer" />
        <h1 className="header__headline">{T.translate('headline')}</h1>
        {this.props.isDevelopment && <h1 className="header__version">{this.props.version}</h1>}
      </header>
    );
  }
}

Header.propTypes = {
  version: PropTypes.string,
  isDevelopment: PropTypes.bool,
  loadVersion: PropTypes.func
};

const mapStateToProps = (state, ownProps) => {
  return {
    version: state.version ? state.version.version : '',
    isDevelopment: state.version ? state.version.isDevelopment : false,
  }
};

const mapDispatchToProps = (dispatch) => {
  return {
    loadVersion: () => dispatch(loadVersion()),
  };
};

export default connect(mapStateToProps, mapDispatchToProps)(Header);
