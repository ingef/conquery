// @flow

import type { DatasetIdType } from '../dataset/reducer';

export { default as reducer } from './reducer';

export { default as Pane }    from './Pane';

export type { StateType }     from './reducer';

export type TabPropsType = {
  selectedDatasetId: DatasetIdType
}
