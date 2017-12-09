// @flow

import { createQueryRunnerActions } from '../query-runner';
import { SET_STATISTICS_FORM }      from './actionTypes';

export const setStatisticsForm = (form: string) => ({
  type: SET_STATISTICS_FORM,
  payload: { form }
});


const {
  startStatisticsQueryStart,
  startStatisticsQueryError,
  startStatisticsQuerySuccess,
  startStatisticsQuery,
  stopStatisticsQueryStart,
  stopStatisticsQueryError,
  stopStatisticsQuerySuccess,
  stopStatisticsQuery,
  queryStatisticsResultStart,
  queryStatisticsResultStop,
  queryStatisticsResultError,
  queryStatisticsResultSuccess,
  queryStatisticsResult,
} = createQueryRunnerActions('statistics', true);

export {
  startStatisticsQueryStart,
  startStatisticsQueryError,
  startStatisticsQuerySuccess,
  startStatisticsQuery,
  stopStatisticsQueryStart,
  stopStatisticsQueryError,
  stopStatisticsQuerySuccess,
  stopStatisticsQuery,
  queryStatisticsResultStart,
  queryStatisticsResultStop,
  queryStatisticsResultError,
  queryStatisticsResultSuccess,
  queryStatisticsResult,
};
