// @flow

import React                        from 'react';
import { connect }                  from 'react-redux';
import T                            from 'i18n-react';
import { IconButton }               from '../button';
import { clearTimebasedQuery }      from './actions';
import { anyConditionFilled }       from './helpers';

type PropsType = {
  clearQuery: () => void,
  isEnabled: boolean,
};

const TimebasedQueryClearButton = (props: PropsType) => {
  return (
    <div className="query-clear-button">
      <IconButton
        onClick={props.clearQuery}
        className="btn btn--small btn--transparent"
        label={T.translate("common.clear")}
        iconClassName="fa-trash-o"
        disabled={!props.isEnabled}
      />
    </div>
  );
};

const mapStateToProps = (state) => ({
  isEnabled: state.panes.right.tabs.timebasedQueryEditor.timebasedQuery.conditions.length > 1 ||
    anyConditionFilled(state.panes.right.tabs.timebasedQueryEditor.timebasedQuery),
});

const mapDispatchToProps = (dispatch) => ({
  clearQuery: () => dispatch(clearTimebasedQuery()),
})

export default connect(mapStateToProps, mapDispatchToProps)(TimebasedQueryClearButton);
