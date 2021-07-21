import type { PreviousQueriesFilterActions } from "../previous-queries/filter/actions";
import type { FolderFilterActions } from "../previous-queries/folderFilter/actions";
import { PreviousQueryListActions } from "../previous-queries/list/actions";
import type { QueriesSearchActions } from "../previous-queries/search/actions";
import { QueryGroupModalActions } from "../query-group-modal/actions";
import { QueryUploadConceptListModalActions } from "../query-upload-concept-list-modal/actions";
import { SnackMessageActions } from "../snack-message/actions";
import type { StandardQueryEditorActions } from "../standard-query-editor/actions";
import { TimebasedActions } from "../timebased-query-editor/actions";
import { UploadConceptListModalActions } from "../upload-concept-list-modal/actions";

type ReduxInitAction = { type: "@@INIT" };

export type Action =
  | ReduxInitAction
  | FolderFilterActions
  | QueriesSearchActions
  | PreviousQueriesFilterActions
  | StandardQueryEditorActions
  | QueryGroupModalActions
  | QueryUploadConceptListModalActions
  | UploadConceptListModalActions
  | SnackMessageActions
  | PreviousQueryListActions
  | TimebasedActions;
