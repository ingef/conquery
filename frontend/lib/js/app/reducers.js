// @flow

import {
  reducer as categoryTrees,
  type StateType as CategoryTreesStateType,
} from '../category-trees';

import {
  reducer as datasets,
  type StateType as DatasetsStateType,
} from '../dataset';

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
import { reducer as form }                     from '../form';
import { reducer as uploadConceptListModal }   from '../upload-concept-list-modal';

import { createQueryRunnerReducer }            from '../query-runner';
import { createQueryNodeModalReducer }         from '../query-node-modal';

export type StateType = {
  categoryTrees: CategoryTreesStateType,
  datasets: DatasetsStateType,
  tooltip: TooltipStateType,
  panes: PanesStateType,
};

export default {
  categoryTrees,
  query,
  uploadConceptListModal,
  standardQueryRunner: createQueryRunnerReducer('standard'),
  timebasedQueryRunner: createQueryRunnerReducer('timebased'),
  queryNodeModal: createQueryNodeModalReducer('standard'),
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
  form,
};
