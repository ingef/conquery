// @flow

import React                    from 'react';
import classnames               from 'classnames';
import type { Dispatch }        from 'redux-thunk';
import T                        from 'i18n-react';
import { connect }              from 'react-redux';
import Markdown                 from 'react-markdown';
import { IconButton }           from '../button';

import ActivateTooltip          from './ActivateTooltip';
import { toggleDisplayTooltip } from './actions';
import type {
  AdditionalInfosType
}                               from './reducer';
import TooltipEntries           from './TooltipEntries';


type PropsType = {
  additionalInfos: AdditionalInfosType,
  displayTooltip: boolean,
  toggleDisplayTooltip: Function,
};

const Tooltip = (props: PropsType) => {
  if (!props.displayTooltip) return <ActivateTooltip />;

  const { additionalInfos, toggleDisplayTooltip } = props;
  const { label, description, infos, matchingEntries, dateRange } = additionalInfos;

  return (
    <div className="tooltip">
      <div className="tooltip__left">
        <div>
          {
            !label && !description &&
            <p className="tooltip__placeholder">
              { T.translate('tooltip.placeholder') }
            </p>
          }
          <h3 className="tooltip__headline">
            {
              label
            } {
              description &&
              <span> - {description}</span>
            }
          </h3>
          {
            infos && infos.map((info, i) => (
              <div className="tooltip-info" key={i}>
                <h3 className="tooltip-info__key" >{info.key}</h3>
                <Markdown className="tooltip-info__value" source={info.value} />
              </div>
            ))
          }
        </div>
      </div>
      <TooltipEntries
        className="tooltip__right"
        matchingEntries={matchingEntries}
        dateRange={dateRange}
      />
      <IconButton
        onClick={toggleDisplayTooltip}
        label={T.translate('tooltip.hide')}
        className={classnames(
          'btn',
          'btn--transparent',
          'btn--transparent--light',
          'btn--tiny',
          'tooltip__btn',
          'tooltip__btn--hide'
        )}
        iconClassName="fa-angle-down"
      />
    </div>
  );
};

const mapStateToProps = (state) => {
  return {
    additionalInfos: state.tooltip.additionalInfos,
    displayTooltip: state.tooltip.displayTooltip,
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => ({
  toggleDisplayTooltip: () => dispatch(toggleDisplayTooltip()),
});

export default connect(mapStateToProps, mapDispatchToProps)(Tooltip);
