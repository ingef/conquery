import type { ConceptIdT } from "../api/types";

// TODO Merge this with actual API type
export type UploadConceptListModalResultType = {
  label: string;
  rootConcepts: unknown;
  resolutionResult: {
    conceptList?: string[];
    filter?: {
      filterId: string;
      tableId: string;
      value: {
        label: string;
        value: string;
      }[];
    };
    selectedRoot: ConceptIdT;
  };
};
