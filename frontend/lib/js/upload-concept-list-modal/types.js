// @flow

import type { TreeNodeIdType } from "../common/types/backend";

export type UploadConceptListModalResultType = {
  label: string,
  rootConcepts: any,
  resolutionResult: {
    conceptList?: string[],
    filter?: {
      filterId: string,
      tableId: string,
      value: {
        label: string,
        value: string
      }[]
    },
    selectedRoot: TreeNodeIdType
  }
};
