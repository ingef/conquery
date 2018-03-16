// @flow

import { combineReducers }                     from 'redux';

import {
  reducer as categoryTrees,
  type StateType as CategoryTreesStateType,
} from '../category-trees';

import {
  reducer as datasets,
  type StateType as DatasetsStateType,
} from '../dataset';

import {
  version,
  type StateType as VersionStateType
} from '../header/reducer'

import {
  reducer as tooltip,
  type StateType as TooltipStateType
} from '../tooltip';

import {
  reducer as panes,
  type StateType as PanesStateType
} from '../pane';

import { reducer as query }                    from '../standard-query-editor';
import { reducer as queryGroupModal }          from '../query-group-modal';
import { reducer as previousQueries }          from '../previous-queries/list';
import { reducer as previousQueriesSearch }    from '../previous-queries/search';
import { reducer as previousQueriesFilter }    from '../previous-queries/filter';
import { reducer as uploadQueryResults }       from '../previous-queries/upload';
import { reducer as deletePreviousQueryModal } from '../previous-queries/delete-modal';
import { reducer as timebasedQuery }           from '../timebased-query-editor';
import { buildExternalFormsReducer }           from '../external-forms';
import { reducer as uploadConceptListModal }   from '../upload-concept-list-modal';

import { createQueryRunnerReducer }            from '../query-runner';
import { createQueryNodeEditorReducer }        from '../query-node-editor';

export type StateType = {
  categoryTrees: CategoryTreesStateType,
  datasets: DatasetsStateType,
  tooltip: TooltipStateType,
  panes: PanesStateType,
  version: VersionStateType,
};

const buildAppReducer = (availableForms) => combineReducers({
  categoryTrees,
  query,
  uploadConceptListModal,
  standardQueryRunner: createQueryRunnerReducer('standard'),
  timebasedQueryRunner: createQueryRunnerReducer('timebased'),
  queryNodeEditor: createQueryNodeEditorReducer('standard'),
  queryGroupModal,
  datasets,
  tooltip,
  panes,
  previousQueries,
  previousQueriesSearch,
  previousQueriesFilter,
  uploadQueryResults,
  deletePreviousQueryModal,
  timebasedQuery,
  version,
  externalForms: buildExternalFormsReducer(availableForms),
});

export default buildAppReducer;
