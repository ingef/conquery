import type { PreviousQueriesFilterActions } from "../previous-queries/filter/actions";
import type { FolderFilterActions } from "../previous-queries/folderFilter/actions";
import type { PreviousQueriesSearchActions } from "../previous-queries/search/actions";
import { QueryGroupModalActions } from "../query-group-modal/actions";
import type { StandardQueryEditorActions } from "../standard-query-editor/actions";

type ReduxInitAction = { type: "@@INIT" };

export type Action =
  | ReduxInitAction
  | FolderFilterActions
  | PreviousQueriesSearchActions
  | PreviousQueriesFilterActions
  | StandardQueryEditorActions
  | QueryGroupModalActions;
