import { ActionType, getType } from "typesafe-actions";

import { Action } from "../app/actions";
import { renameQuery } from "../previous-queries/list/actions";

import {
  addTimebasedCondition,
  clearTimebasedQuery,
  dropTimebasedNode,
  removeTimebasedCondition,
  removeTimebasedNode,
  setTimebasedConditionMaxDays,
  setTimebasedConditionMinDays,
  setTimebasedConditionMinDaysOrNoEvent,
  setTimebasedConditionOperator,
  setTimebasedIndexResult,
  setTimebasedNodeTimestamp,
} from "./actions";

export type TimebasedTimestampType = "EARLIEST" | "LATEST" | "RANDOM";

export type TimebasedResultType = {
  id: string;
  label: string;
  timestamp: TimebasedTimestampType;
};

export type TimebasedOperatorType =
  | "BEFORE"
  | "BEFORE_OR_SAME"
  | "SAME"
  | "DAYS_BEFORE"
  | "DAYS_OR_NO_EVENT_BEFORE";

export const TIMEBASED_OPERATOR_TYPES: TimebasedOperatorType[] = [
  "BEFORE",
  "BEFORE_OR_SAME",
  "SAME",
  "DAYS_BEFORE",
  "DAYS_OR_NO_EVENT_BEFORE",
];

export interface TimebasedConditionT {
  operator: TimebasedOperatorType;
  result0: TimebasedResultType | null;
  result1: TimebasedResultType | null;
  minDays?: number | null;
  maxDays?: number | null;
  minDaysOrNoEvent?: number | null;
}

export interface ValidatedTimebasedConditionT extends TimebasedConditionT {
  result0: TimebasedResultType;
  result1: TimebasedResultType;
}

export interface TimebasedQueryStateT {
  indexResult: string | null;
  conditions: TimebasedConditionT[];
}

const getEmptyNode = () => ({
  operator: "BEFORE" as const,
  result0: null,
  result1: null,
});

const setTimebasedConditionAttributes = (
  state: TimebasedQueryStateT,
  conditionIdx: number,
  attributes: Partial<TimebasedConditionT>,
) => {
  return {
    ...state,
    conditions: [
      ...state.conditions.slice(0, conditionIdx),
      {
        ...state.conditions[conditionIdx],
        ...attributes,
      },
      ...state.conditions.slice(conditionIdx + 1),
    ],
  };
};

const setNode = (
  state: TimebasedQueryStateT,
  resultIdx: number,
  conditionIdx: number,
  node: TimebasedResultType,
) => {
  const attributes = {
    [`result${resultIdx}`]: {
      ...node,
      timestamp: node.timestamp || "EARLIEST",
    },
  };

  return setTimebasedConditionAttributes(state, conditionIdx, attributes);
};

const conditionResultsToArray = (conditions: TimebasedConditionT[]) => {
  return conditions.reduce<TimebasedResultType[]>((results, c) => {
    if (c.result0) results.push(c.result0);
    if (c.result1) results.push(c.result1);

    return results;
  }, []);
};

const getPossibleIndexResults = (conditions: TimebasedConditionT[]) => {
  return conditions.reduce<TimebasedResultType[]>(
    (possibleResults, condition) => {
      if (condition.operator === "DAYS_OR_NO_EVENT_BEFORE" && condition.result1)
        possibleResults.push(condition.result1);

      if (condition.operator !== "DAYS_OR_NO_EVENT_BEFORE" && condition.result0)
        possibleResults.push(condition.result0);

      if (condition.operator !== "DAYS_OR_NO_EVENT_BEFORE" && condition.result1)
        possibleResults.push(condition.result1);

      return possibleResults;
    },
    [],
  );
};

const ensureIndexResult = (state: TimebasedQueryStateT) => {
  // Return if there is already an indexResult
  if (state.indexResult) return state;

  // Ok, so there is none, let's ensure it
  const possibleResults = getPossibleIndexResults(state.conditions);
  // allResults includes results on the left side
  // of conditions with a DAYS_OR_NO_EVENT_BEFORE operator
  const allResults = conditionResultsToArray(state.conditions);

  // Best case
  if (possibleResults.length > 0)
    return { ...state, indexResult: possibleResults[0].id };

  // Bad but ok case
  if (allResults.length > 0) return { ...state, indexResult: allResults[0].id };

  // Well, couldn't find any result
  return { ...state, indexResult: null };
};

const onDropTimebasedNode = (
  state: TimebasedQueryStateT,
  {
    node,
    resultIdx,
    conditionIdx,
    moved,
  }: ActionType<typeof dropTimebasedNode>["payload"],
) => {
  const stateWithNode = setNode(state, resultIdx, conditionIdx, node);

  return moved ? stateWithNode : ensureIndexResult(stateWithNode);
};

const onRemoveTimebasedNode = (
  state: TimebasedQueryStateT,
  {
    conditionIdx,
    resultIdx,
    moved,
  }: ActionType<typeof removeTimebasedNode>["payload"],
) => {
  const condition = state.conditions[conditionIdx];
  const node = resultIdx === 0 ? condition.result0 : condition.result1;

  if (!node) return state;

  const attributes = {
    [`result${resultIdx}`]: null,
  };

  const stateWithoutNode = setTimebasedConditionAttributes(
    state,
    conditionIdx,
    attributes,
  );

  if (moved) return stateWithoutNode;

  // If item has not been moved and was indexResult, remove indexResult
  const nextState =
    node.id === state.indexResult
      ? { ...stateWithoutNode, indexResult: null }
      : stateWithoutNode;

  return ensureIndexResult(nextState);
};

const onSetTimebasedNodeTimestamp = (
  state: TimebasedQueryStateT,
  {
    conditionIdx,
    resultIdx,
    timestamp,
  }: ActionType<typeof setTimebasedNodeTimestamp>["payload"],
) => {
  const condition = state.conditions[conditionIdx];
  const node = resultIdx === 0 ? condition.result0 : condition.result1;
  const attributes = {
    [`result${resultIdx}`]: {
      ...node,
      timestamp,
    },
  };

  return setTimebasedConditionAttributes(state, conditionIdx, attributes);
};

const onSetTimebasedConditionOperator = (
  state: TimebasedQueryStateT,
  {
    conditionIdx,
    operator,
  }: ActionType<typeof setTimebasedConditionOperator>["payload"],
) => {
  // Check if we're not switching to DAYS_OR_NO_EVENT_BEFORE. Then we're good.
  // But IF IN FACT we do, check if the indexResult is somewhere else than on the left result
  // Then we're also good.
  const nextState = setTimebasedConditionAttributes(state, conditionIdx, {
    operator,
  });

  if (
    operator !== "DAYS_OR_NO_EVENT_BEFORE" ||
    !state.conditions[conditionIdx].result0 ||
    state.conditions[conditionIdx].result0.id !== state.indexResult
  )
    return nextState;

  // Now that this didn't work we're switching the operator to DAYS_OR_NO_EVENT_BEFORE
  // and the indexResult points to the first of both results of this condition.
  // This is not allowed with DAYS_OR_NO_EVENT_BEFORE.

  // Let's try to find a possible result to be the new indexResult
  const possibleResults = getPossibleIndexResults(nextState.conditions);

  // Now let's hope we found a possible result
  return possibleResults.length === 0
    ? // Too bad, couldn't find a possible result
      nextState
    : // Nice, take the first result
      {
        ...nextState,
        indexResult: possibleResults[0].id,
      };
};

const onSetTimebasedConditionMinDays = (
  state: TimebasedQueryStateT,
  {
    days,
    conditionIdx,
  }: ActionType<typeof setTimebasedConditionMinDays>["payload"],
) => {
  return setTimebasedConditionAttributes(state, conditionIdx, {
    minDays: days,
  });
};

const onSetTimebasedConditionMaxDays = (
  state: TimebasedQueryStateT,
  {
    days,
    conditionIdx,
  }: ActionType<typeof setTimebasedConditionMaxDays>["payload"],
) => {
  return setTimebasedConditionAttributes(state, conditionIdx, {
    maxDays: days,
  });
};

const onSetTimebasedConditionMinDaysOrNoEvent = (
  state: TimebasedQueryStateT,
  {
    days,
    conditionIdx,
  }: ActionType<typeof setTimebasedConditionMinDaysOrNoEvent>["payload"],
) => {
  return setTimebasedConditionAttributes(state, conditionIdx, {
    minDaysOrNoEvent: days,
  });
};

const onSetTimebasedIndexResult = (
  state: TimebasedQueryStateT,
  { indexResult }: ActionType<typeof setTimebasedIndexResult>["payload"],
) => {
  return {
    ...state,
    indexResult,
  };
};

const onAddTimebasedCondition = (state: TimebasedQueryStateT) => {
  return {
    ...state,
    conditions: [...state.conditions, getEmptyNode()],
  };
};

const onRemoveTimebasedCondition = (
  state: TimebasedQueryStateT,
  { conditionIdx }: ActionType<typeof removeTimebasedCondition>["payload"],
) => {
  const deletedCondition = state.conditions[conditionIdx];
  const nextState = {
    ...state,
    conditions: [
      ...state.conditions.slice(0, conditionIdx),
      ...state.conditions.slice(conditionIdx + 1),
    ],
  };

  // if no node was indexed
  if (
    !(
      deletedCondition.result0 &&
      deletedCondition.result0.id === state.indexResult
    ) &&
    !(
      deletedCondition.result1 &&
      deletedCondition.result1.id === state.indexResult
    )
  )
    return nextState;

  return ensureIndexResult({ ...nextState, indexResult: null });
};

const renameQueries = (
  state: TimebasedQueryStateT,
  { queryId, label }: ActionType<typeof renameQuery.success>["payload"],
) => {
  return {
    ...state,
    conditions: state.conditions.map((c) => {
      const result0 =
        c.result0 && c.result0.id === queryId
          ? { ...c.result0, label: label }
          : c.result0;

      const result1 =
        c.result1 && c.result1.id === queryId
          ? { ...c.result1, label: label }
          : c.result1;

      return {
        ...c,
        result0,
        result1,
      };
    }),
  };
};

const initialState = {
  indexResult: null,
  conditions: [getEmptyNode()],
};

// This state contains multiple and-conditions.
// Every and-condition sets exactly two previous queries in a time-based relation.
// To be more specific, the RESULTS of two previous queries are set in relation.
// (The relation is measured in days)
//
// The result of this timebased query will look like those from any other query:
//
// A number of results with
//   id,
//   first date of occurrence,
//   last date of occurrence,
//   random dateof occurrence
//
// These dates will have to be taken from a single previous query
// (result1 or result2 from within specific condition) the "indexResult".
//
// Example:
//
// {
//   indexResult: 2523,
//   conditions: [
//     {
//       operator: 'BEFORE',
//       // operator: 'BEFORE_OR_SAME',
//       // operator: 'SAME',
//       result0: {
//         id: 2523,
//         timestamp: 'FIRST',
//         // timestamp: 'LAST',
//         // timestamp: 'RANDOM',
//       },
//       result1: {
//         id: 2525,
//         timestamp: 'LAST'
//       },
//       minDays: 5,
//       maxDays: 10
//     },
//     {
//       operator: 'SAME',
//       result0: { id: 6274, timestamp: "RANDOM" },
//       result1: { id: 5274, timestamp: "RANDOM" },
//     }
//   ]
// }
const timebasedQuery = (
  state: TimebasedQueryStateT = initialState,
  action: Action,
): TimebasedQueryStateT => {
  switch (action.type) {
    case getType(dropTimebasedNode):
      return onDropTimebasedNode(state, action.payload);
    case getType(removeTimebasedNode):
      return onRemoveTimebasedNode(state, action.payload);
    case getType(setTimebasedNodeTimestamp):
      return onSetTimebasedNodeTimestamp(state, action.payload);
    case getType(setTimebasedConditionOperator):
      return onSetTimebasedConditionOperator(state, action.payload);
    case getType(setTimebasedConditionMinDays):
      return onSetTimebasedConditionMinDays(state, action.payload);
    case getType(setTimebasedConditionMaxDays):
      return onSetTimebasedConditionMaxDays(state, action.payload);
    case getType(setTimebasedConditionMinDaysOrNoEvent):
      return onSetTimebasedConditionMinDaysOrNoEvent(state, action.payload);
    case getType(setTimebasedIndexResult):
      return onSetTimebasedIndexResult(state, action.payload);
    case getType(addTimebasedCondition):
      return onAddTimebasedCondition(state);
    case getType(removeTimebasedCondition):
      return onRemoveTimebasedCondition(state, action.payload);
    case getType(renameQuery.success):
      return renameQueries(state, action.payload);
    case getType(clearTimebasedQuery):
      return initialState;
    default:
      return state;
  }
};

export default timebasedQuery;
