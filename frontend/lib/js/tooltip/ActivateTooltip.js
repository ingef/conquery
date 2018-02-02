import React                    from 'react';
import PropTypes                from 'prop-types';
import T                        from 'i18n-react';
import { connect }              from 'react-redux';
import { IconButton }           from '../button';
import { toggleDisplayTooltip } from './actions';

const ActivateTooltip = (props) => {
  return (
    <div className="tooltip tooltip--activate">
      <IconButton
        iconClassName="fa-angle-up"
        className="btn btn--transparent btn--tiny tooltip__btn tooltip__btn--show"
        onClick={props.toggleDisplayTooltip}
        label={T.translate('tooltip.show')}
      />
    </div>
  );
};

ActivateTooltip.propTypes = {
  toggleDisplayTooltip: PropTypes.func.isRequired,
};

const mapDispatchToProps = (dispatch) => ({
  toggleDisplayTooltip: () => dispatch(toggleDisplayTooltip()),
});

export default connect(() => ({}), mapDispatchToProps)(ActivateTooltip);
