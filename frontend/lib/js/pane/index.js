// @flow

import type { DatasetIdT } from "../api/types";

export { default as Pane } from "./Pane";

export type { StateType } from "./reducer";

export type TabPropsType = {
  selectedDatasetId: DatasetIdT
};
