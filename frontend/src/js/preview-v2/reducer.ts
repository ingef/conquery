import { AsyncRecordBatchStreamReader, RecordBatch } from "apache-arrow";
import { getType } from "typesafe-actions";
import { GetQueryResponseT, PreviewStatisticsResponse } from "../api/types";

import { Action } from "../app/actions";

import {
  closePreview,
  loadPreview,
  openPreview,
  updateQueryId,
} from "./actions";

export type PreviewStateT = {
  isOpen: boolean;
  isLoading: boolean;
  dataLoadedForQueryId: string | null;
  statisticsData: PreviewStatisticsResponse | null;
  queryData: GetQueryResponseT | null;
  arrowReader: AsyncRecordBatchStreamReader | null;
  initialTableData: IteratorResult<RecordBatch> | null;
  lastQuery: string | null;
};

const initialState: PreviewStateT = {
  isOpen: false,
  isLoading: false,
  dataLoadedForQueryId: null,
  statisticsData: null,
  queryData: null,
  arrowReader: null,
  initialTableData: null,
  lastQuery: null,
};

export default function reducer(
  state: PreviewStateT = initialState,
  action: Action,
): PreviewStateT {
  switch (action.type) {
    case getType(openPreview):
      return {
        ...state,
        isOpen: true,
      };
    case getType(closePreview):
      return {
        ...state,
        isOpen: false,
      };
    case getType(loadPreview.request):
      return {
        ...state,
        isLoading: true,
      };
    case getType(loadPreview.failure):
      return {
        ...state,
        dataLoadedForQueryId: null,
        statisticsData: null,
        arrowReader: null,
        initialTableData: null,
        isLoading: false,
      };
    case getType(loadPreview.success):
      return {
        ...state,
        isLoading: false,
        dataLoadedForQueryId: action.payload.queryId,
        queryData: action.payload.queryData,
        arrowReader: action.payload.arrowReader,
        initialTableData: action.payload.initialTableData,
        statisticsData: action.payload.statisticsData,
      };
    case getType(updateQueryId):
      return {
        ...state,
        lastQuery: action.payload.queryId,
      };
    default:
      return state;
  }
}
