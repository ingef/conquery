// @flow

import React                    from 'react';
import { connect }              from 'react-redux';
import T                        from 'i18n-react';

import {
  addTimebasedCondition,
  removeTimebasedCondition,
  setTimebasedConditionOperator,
  dropTimebasedNode,
  setTimebasedNodeTimestamp,
  removeTimebasedNode,
  setTimebasedIndexResult,
  setTimebasedConditionMinDays,
  setTimebasedConditionMaxDays,
  setTimebasedConditionMinDaysOrNoEvent,
  setTimebasedConditionMaxDaysOrNoEvent,
} from './actions';

import TimebasedCondition       from './TimebasedCondition';

type PropsType = {
  query: Object,
  onDropTimebasedNode: () => void,
  onRemoveTimebasedNode: () => void,
  onAddTimebasedCondition: () => void,
  onRemoveTimebasedCondition: () => void,
  onSetTimebasedConditionOperator: () => void,
  onSetTimebasedNodeTimestamp: () => void,
  onSetTimebasedIndexResult: () => void,
  onSetTimebasedConditionMinDays: () => void,
  onSetTimebasedConditionMaxDays: () => void,
  onSetTimebasedConditionMinDaysOrNoEvent: () => void,
  onSetTimebasedConditionMaxDaysOrNoEvent: () => void,
};

const TimebasedQueryEditor = (props: PropsType) => {
  return (
    <div className="timebased-query-editor">
      {
        props.query.conditions.map((condition, idx) => [
          <TimebasedCondition
            key={idx}
            condition={condition}
            conditionIdx={idx}
            indexResult={props.query.indexResult}
            removable={props.query.conditions.length > 1}
            onRemove={() => props.onRemoveTimebasedCondition(idx)}
            onRemoveTimebasedNode={(resultIdx, moved) => {
              props.onRemoveTimebasedNode(idx, resultIdx, moved);
            }}
            onSetOperator={(value) => props.onSetTimebasedConditionOperator(idx, value)}
            onDropTimebasedNode={(resultIdx, node, moved) => {
              props.onDropTimebasedNode(idx, resultIdx, node, moved);
            }}
            onSetTimebasedNodeTimestamp={(resultIdx, timestamp) => {
              props.onSetTimebasedNodeTimestamp(idx, resultIdx, timestamp);
            }}
            onSetTimebasedIndexResult={props.onSetTimebasedIndexResult}
            onSetTimebasedConditionMinDays={(days) => {
              props.onSetTimebasedConditionMinDays(idx, days);
            }}
            onSetTimebasedConditionMaxDays={(days) => {
              props.onSetTimebasedConditionMaxDays(idx, days);
            }}
            onSetTimebasedConditionMinDaysOrNoEvent={(days) => {
              props.onSetTimebasedConditionMinDaysOrNoEvent(idx, days);
            }}
            onSetTimebasedConditionMaxDaysOrNoEvent={(days) => {
              props.onSetTimebasedConditionMaxDaysOrNoEvent(idx, days);
            }}
          />,
          <p className="timebased-query-editor__connector">{T.translate('common.and')}</p>
        ])
      }
      <button
        onClick={props.onAddTimebasedCondition}
        className="timebased-query-editor__add-btn btn btn--transparent btn--icon"
      >
        <i className="fa fa-plus" /> { T.translate('timebasedQueryEditor.addCondition')}
      </button>
    </div>
  );
};


const mapStateToProps = (state) => ({
  query: state.panes.right.tabs.timebasedQueryEditor.timebasedQuery
});

const mapDispatchToProps = (dispatch) => ({
  onAddTimebasedCondition: () => dispatch(addTimebasedCondition()),
  onRemoveTimebasedCondition: (idx) => dispatch(removeTimebasedCondition(idx)),
  onSetTimebasedConditionOperator: (idx, value) =>
    dispatch(setTimebasedConditionOperator(idx, value)),
  onDropTimebasedNode: (conditionIdx, resultIdx, node, moved) =>
    dispatch(dropTimebasedNode(conditionIdx, resultIdx, node, moved)),
  onSetTimebasedNodeTimestamp: (conditionIdx, resultIdx, timestamp) =>
    dispatch(setTimebasedNodeTimestamp(conditionIdx, resultIdx, timestamp)),
  onRemoveTimebasedNode: (conditionIdx, resultIdx, moved) =>
    dispatch(removeTimebasedNode(conditionIdx, resultIdx, moved)),
  onSetTimebasedIndexResult: (indexResult) =>
    dispatch(setTimebasedIndexResult(indexResult)),
  onSetTimebasedConditionMinDays: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMinDays(conditionIdx, days)),
  onSetTimebasedConditionMaxDays: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMaxDays(conditionIdx, days)),
  onSetTimebasedConditionMinDaysOrNoEvent: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMinDaysOrNoEvent(conditionIdx, days)),
  onSetTimebasedConditionMaxDaysOrNoEvent: (conditionIdx, days) =>
    dispatch(setTimebasedConditionMaxDaysOrNoEvent(conditionIdx, days)),
});

export default connect(mapStateToProps, mapDispatchToProps)(TimebasedQueryEditor);
