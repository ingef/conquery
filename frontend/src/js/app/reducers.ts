import { combineReducers } from "redux";

import conceptTrees, { ConceptTreesStateT } from "../concept-trees/reducer";

import datasets, { DatasetStateT } from "../dataset/reducer";
import tooltip, { TooltipStateT } from "../tooltip/reducer";

import user, { UserStateT } from "../user/reducer";

import type { TabT } from "../pane/types";

import startup, { StartupStateT } from "../startup/reducer";
import { buildPanesReducer, PanesStateT } from "../pane/reducer";
import queryGroupModal from "../query-group-modal/reducer";
import previousQueries, {
  PreviousQueriesStateT
} from "../previous-queries/list/reducer";
import previousQueriesSearch, {
  PreviousQueriesSearchStateT
} from "../previous-queries/search/reducer";
import previousQueriesFilter, {
  PreviousQueriesFilterStateT
} from "../previous-queries/filter/reducer";
import uploadQueryResults from "../previous-queries/upload/reducer";
import deletePreviousQueryModal from "../previous-queries/delete-modal/reducer";
import snackMessage from "../snack-message/reducer";
import preview from "../preview/reducer";
import queryUploadConceptListModal from "../query-upload-concept-list-modal/reducer";
import uploadConceptListModal, {
  UploadConceptListModalStateT
} from "../upload-concept-list-modal/reducer";

import { createQueryNodeEditorReducer } from "../query-node-editor/reducer";

import type { StandardQueryEditorStateT } from "../standard-query-editor";

// TODO: Introduce more StateTypes gradually
export type StateT = {
  conceptTrees: ConceptTreesStateT;
  datasets: DatasetStateT;
  tooltip: TooltipStateT;
  panes: PanesStateT;
  uploadConceptListModal: UploadConceptListModalStateT;
  user: UserStateT;
  queryEditor: StandardQueryEditorStateT;
  startup: StartupStateT;
  previousQueries: PreviousQueriesStateT;
  previousQueriesSearch: PreviousQueriesSearchStateT;
  previousQueriesFilter: PreviousQueriesFilterStateT;
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
