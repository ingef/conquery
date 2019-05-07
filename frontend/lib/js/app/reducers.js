// @flow

import { combineReducers } from "redux";

import categoryTrees, {
  type StateType as CategoryTreesStateType
} from "../category-trees/reducer";

import {
  reducer as datasets,
  type StateType as DatasetsStateType
} from "../dataset";

import {
  reducer as tooltip,
  type StateType as TooltipStateType
} from "../tooltip";

import uploadConceptListModal, {
  type StateType as UploadConceptListModalStateType
} from "../upload-concept-list-modal/reducer";

import { type StateType as PanesStateType } from "../pane";

import { reducer as startup } from "../startup";
import { buildPanesReducer } from "../pane/reducer";
import { reducer as queryGroupModal } from "../query-group-modal";
import { reducer as previousQueries } from "../previous-queries/list";
import { reducer as previousQueriesSearch } from "../previous-queries/search";
import { reducer as previousQueriesFilter } from "../previous-queries/filter";
import { reducer as uploadQueryResults } from "../previous-queries/upload";
import { reducer as deletePreviousQueryModal } from "../previous-queries/delete-modal";
import { reducer as uploadFilterListModal } from "../upload-filter-list-modal";
import { reducer as snackMessage } from "../snack-message";

import { createQueryNodeEditorReducer } from "../query-node-editor";

export type StateType = {
  categoryTrees: CategoryTreesStateType,
  datasets: DatasetsStateType,
  tooltip: TooltipStateType,
  panes: PanesStateType,
  uploadConceptListModal: UploadConceptListModalStateType
};

const buildAppReducer = tabs =>
  combineReducers({
    startup,
    categoryTrees,
    uploadConceptListModal,
    uploadFilterListModal,
    queryNodeEditor: createQueryNodeEditorReducer("standard"),
    queryGroupModal,
    datasets,
    tooltip,
    panes: buildPanesReducer(tabs),
    previousQueries,
    previousQueriesSearch,
    previousQueriesFilter,
    uploadQueryResults,
    deletePreviousQueryModal,
    snackMessage
  });

export default buildAppReducer;
