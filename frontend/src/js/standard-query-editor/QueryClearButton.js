import React, { PropTypes }        from 'react';
import { connect }                 from 'react-redux';
import T                           from 'i18n-react';

import { IconButton }              from '../button';

import { clearQuery }              from './actions';

const QueryClearButton = (props) => {
  return props.isVisible && (
    <div className="query-clear-button">
      <IconButton
        onClick={props.clearQuery}
        className="btn btn--small btn--transparent"
        label={T.translate("common.clear")}
        iconClassName="fa-trash-o"
      />
    </div>
  );
};

QueryClearButton.propTypes = {
  clearQuery: PropTypes.func.isRequired,
  isVisible: PropTypes.bool.isRequired
};

const mapStateToProps = (state) => ({
  isVisible: state.query.length !== 0,
});

const mapDispatchToProps = (dispatch) => ({
  clearQuery: () => dispatch(clearQuery()),
});

export default connect(mapStateToProps, mapDispatchToProps)(QueryClearButton);
