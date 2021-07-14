import type { TimebasedQueryStateT } from "./reducer";

export const allConditionsFilled = (timebasedQuery: TimebasedQueryStateT) =>
  timebasedQuery.conditions.every(
    (condition) => !!condition.result0 && !!condition.result1,
  );

export const anyConditionFilled = (timebasedQuery: TimebasedQueryStateT) =>
  timebasedQuery.conditions.some(
    (condition) => condition.result0 || condition.result1,
  );
