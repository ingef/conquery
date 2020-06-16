// Action structure is compliant with FSA: https://github.com/acdlite/flux-standard-action
// in case the action returns an object
//
// In the second case, an action can be a function that is processed by redux-thunk
// to support asynchronous calls, e.g. API calls.

import * as genericActions from "../common/actions";
import * as conceptTreeActions from "../concept-trees/actions";
import * as datasetSelectorActions from "../dataset/actions";
import * as queryGroupModalActions from "../query-group-modal/actions";
import * as additionalInfosActions from "../tooltip/actions";
import * as paneActions from "../pane/actions";
import * as previousQueriesActions from "../previous-queries/list/actions";
import * as previousQueriesSearchActions from "../previous-queries/search/actions";
import * as previousQueriesFilterActions from "../previous-queries/filter/actions";
import * as timebasedQueryEditorActions from "../timebased-query-editor/actions";
import * as queryEditorActions from "../standard-query-editor/actions";

import createQueryRunnerActions from "../query-runner/actions";

const standardQueryRunnerActions = createQueryRunnerActions("standard");
const timebasedQueryRunnerActions = createQueryRunnerActions("timebased");
const externalQueryRunnerActions = createQueryRunnerActions("external");

export default {
  ...genericActions,
  ...conceptTreeActions,
  ...queryEditorActions,
  ...standardQueryRunnerActions,
  ...timebasedQueryRunnerActions,
  ...datasetSelectorActions,
  ...queryGroupModalActions,
  ...additionalInfosActions,
  ...paneActions,
  ...previousQueriesActions,
  ...previousQueriesSearchActions,
  ...previousQueriesFilterActions,
  ...timebasedQueryEditorActions,
  ...externalQueryRunnerActions,
};
