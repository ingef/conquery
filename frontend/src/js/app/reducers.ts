import { combineReducers } from "redux";

import conceptTreesOpen, {
  ConceptTreesOpenStateT,
} from "../concept-trees-open/reducer";
import conceptTrees, { ConceptTreesStateT } from "../concept-trees/reducer";
import datasets, { DatasetStateT } from "../dataset/reducer";
import panes, { PanesStateT } from "../pane/reducer";
import type { TabT } from "../pane/types";
import preview, { PreviewStateT } from "../preview/reducer";
import projectItemsFilter, {
  ProjectItemsFilterStateT,
} from "../previous-queries/filter/reducer";
import previousQueriesFolderFilter, {
  PreviousQueriesFolderFilterStateT,
} from "../previous-queries/folder-filter/reducer";
import previousQueries, {
  PreviousQueriesStateT,
} from "../previous-queries/list/reducer";
import projectItemsSearch, {
  ProjectItemsSearchStateT,
} from "../previous-queries/search/reducer";
import projectItemsTypeFilter, {
  ProjectItemsTypeFilterStateT,
} from "../previous-queries/type-filter/reducer";
import {
  createQueryNodeEditorReducer,
  QueryNodeEditorStateT,
} from "../query-node-editor/reducer";
import queryUploadConceptListModal, {
  QueryUploadConceptListModalStateT,
} from "../query-upload-concept-list-modal/reducer";
import snackMessage, { SnackMessageStateT } from "../snack-message/reducer";
import type { StandardQueryEditorStateT } from "../standard-query-editor";
import startup, { StartupStateT } from "../startup/reducer";
import tooltip, { TooltipStateT } from "../tooltip/reducer";
import uploadConceptListModal, {
  UploadConceptListModalStateT,
} from "../upload-concept-list-modal/reducer";
import user, { UserStateT } from "../user/reducer";

// TODO: Introduce more StateTypes gradually
export type StateT = {
  conceptTrees: ConceptTreesStateT;
  conceptTreesOpen: ConceptTreesOpenStateT;
  datasets: DatasetStateT;
  tooltip: TooltipStateT;
  panes: PanesStateT;
  uploadConceptListModal: UploadConceptListModalStateT;
  queryUploadConceptListModal: QueryUploadConceptListModalStateT;
  user: UserStateT;
  queryEditor: StandardQueryEditorStateT;
  queryNodeEditor: QueryNodeEditorStateT;
  startup: StartupStateT;
  previousQueries: PreviousQueriesStateT;
  projectItemsSearch: ProjectItemsSearchStateT;
  projectItemsFilter: ProjectItemsFilterStateT;
  projectItemsTypeFilter: ProjectItemsTypeFilterStateT;
  previousQueriesFolderFilter: PreviousQueriesFolderFilterStateT;
  preview: PreviewStateT;
  snackMessage: SnackMessageStateT;
};

const buildAppReducer = (tabs: TabT[]) => {
  return combineReducers({
    startup,
    conceptTrees,
    conceptTreesOpen,
    uploadConceptListModal,
    queryUploadConceptListModal,
    queryNodeEditor: createQueryNodeEditorReducer("standard"),
    datasets,
    tooltip,
    panes,
    previousQueries,
    projectItemsSearch,
    projectItemsFilter,
    projectItemsTypeFilter,
    previousQueriesFolderFilter,
    snackMessage,
    preview,
    user,
    ...tabs.reduce((all, tab) => {
      all[tab.key] = tab.reducer;
      return all;
    }, {}),
  });
};

export default buildAppReducer;
