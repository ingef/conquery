import { combineReducers } from "redux";

import conceptTreesOpen, {
  ConceptTreesOpenStateT,
} from "../concept-trees-open/reducer";
import conceptTrees, { ConceptTreesStateT } from "../concept-trees/reducer";
import datasets, { DatasetStateT } from "../dataset/reducer";
import formConfigsFilter, {
  FormConfigsFilterStateT,
} from "../external-forms/form-configs/filter/reducer";
import formConfigs, {
  FormConfigsStateT,
} from "../external-forms/form-configs/reducer";
import formConfigsSearch, {
  FormConfigsSearchStateT,
} from "../external-forms/form-configs/search/reducer";
import panes, { PanesStateT } from "../pane/reducer";
import type { TabT } from "../pane/types";
import preview, { PreviewStateT } from "../preview/reducer";
import previousQueriesFilter, {
  PreviousQueriesFilterStateT,
} from "../previous-queries/filter/reducer";
import previousQueriesFolderFilter, {
  PreviousQueriesFolderFilterStateT,
} from "../previous-queries/folderFilter/reducer";
import previousQueries, {
  PreviousQueriesStateT,
} from "../previous-queries/list/reducer";
import previousQueriesSearch, {
  QueriesSearchStateT,
} from "../previous-queries/search/reducer";
import uploadQueryResults, {
  UploadQueryResultsStateT,
} from "../previous-queries/upload/reducer";
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
  uploadQueryResults: UploadQueryResultsStateT;
  user: UserStateT;
  queryEditor: StandardQueryEditorStateT;
  queryNodeEditor: QueryNodeEditorStateT;
  startup: StartupStateT;
  previousQueries: PreviousQueriesStateT;
  previousQueriesSearch: QueriesSearchStateT;
  previousQueriesFilter: PreviousQueriesFilterStateT;
  previousQueriesFolderFilter: PreviousQueriesFolderFilterStateT;
  formConfigs: FormConfigsStateT;
  formConfigsSearch: FormConfigsSearchStateT;
  formConfigsFilter: FormConfigsFilterStateT;
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
    previousQueriesSearch,
    previousQueriesFilter,
    previousQueriesFolderFilter,
    uploadQueryResults,
    snackMessage,
    preview,
    user,
    formConfigs,
    formConfigsSearch,
    formConfigsFilter,
    ...tabs.reduce((all, tab) => {
      all[tab.key] = tab.reducer;
      return all;
    }, {}),
  });
};

export default buildAppReducer;
