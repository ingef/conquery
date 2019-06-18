// @flow

import type { ConceptIdT } from "../api/types";

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
    selectedRoot: ConceptIdT
  }
};
