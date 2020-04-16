import { combineReducers } from "redux";

import conceptTrees, {
  StateType as ConceptTreesStateType
} from "../concept-trees/reducer";

import {
  default as datasets,
  StateType as DatasetsStateType
} from "../dataset/reducer";

import {
  default as tooltip,
  StateType as TooltipStateType
} from "../tooltip/reducer";

import uploadConceptListModal, {
  StateType as UploadConceptListModalStateType
} from "../upload-concept-list-modal/reducer";

import user, { StateType as UserStateT } from "../user/reducer";

import type { StateType as PanesStateType } from "../pane";
import type { TabT } from "../pane/types";

import { default as startup } from "../startup/reducer";
import { buildPanesReducer } from "../pane/reducer";
import { default as queryGroupModal } from "../query-group-modal/reducer";
import { default as previousQueries } from "../previous-queries/list/reducer";
import { default as previousQueriesSearch } from "../previous-queries/search/reducer";
import { default as previousQueriesFilter } from "../previous-queries/filter/reducer";
import { reducer as uploadQueryResults } from "../previous-queries/upload";
import { default as deletePreviousQueryModal } from "../previous-queries/delete-modal/reducer";
import { default as snackMessage } from "../snack-message/reducer";
import { default as preview } from "../preview/reducer";
import { default as queryUploadConceptListModal } from "../query-upload-concept-list-modal/reducer";

import { createQueryNodeEditorReducer } from "../query-node-editor/reducer";

import type { StandardQueryEditorStateT } from "../standard-query-editor";
import type { StartupStateT } from "../startup/reducer";

// TODO: Introduce more StateTypes gradually
export type StateType = {
  conceptTrees: ConceptTreesStateType,
  datasets: DatasetsStateType,
  tooltip: TooltipStateType,
  panes: PanesStateType,
  uploadConceptListModal: UploadConceptListModalStateType,
  user: UserStateT,
  queryEditor: StandardQueryEditorStateT,
  startup: StartupStateT
};

const buildAppReducer = (tabs: TabT[]) => {
  return combineReducers({
    startup,
    conceptTrees,
    uploadConceptListModal,
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
    snackMessage,
    preview,
    queryUploadConceptListModal,
    user,
    ...tabs.reduce((all, tab) => {
      all[tab.key] = tab.reducer;
      return all;
    }, {})
  });
};

export default buildAppReducer;
