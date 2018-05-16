// @flow

import { type TreeNodeIdType } from '../common/types/backend';

export const UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL =
  "upload-concept-list-modal/UPLOAD_CONCEPT_LIST_MODAL_UPDATE_LABEL";

export const SELECT_CONCEPT_ROOT_NODE = "upload-concept-list-modal/SELECT_CONCEPT_ROOT_NODE";

export const RESOLVE_CONCEPTS_START   = "upload-concept-list-modal/RESOLVE_CONCEPTS_START";
export const RESOLVE_CONCEPTS_SUCCESS = "upload-concept-list-modal/RESOLVE_CONCEPTS_SUCCESS";
export const RESOLVE_CONCEPTS_ERROR   = "upload-concept-list-modal/RESOLVE_CONCEPTS_ERROR";

export const RESOLVE_FILTER_VALUES_SUCCESS =
  "upload-concept-list-modal/RESOLVE_FILTER_VALUES_SUCCESS";
export const RESOLVE_FILTER_VALUES_ERROR =
  "upload-concept-list-modal/RESOLVE_FILTER_VALUES_ERROR";

export const UPLOAD_CONCEPT_LIST_MODAL_OPEN   =
  "upload-concept-list-modal/UPLOAD_CONCEPT_LIST_MODAL_OPEN";
export const UPLOAD_CONCEPT_LIST_MODAL_CLOSE  =
  "upload-concept-list-modal/UPLOAD_CONCEPT_LIST_MODAL_CLOSE";
export const UPLOAD_CONCEPT_LIST_MODAL_ACCEPT =
  "upload-concept-list-modal/UPLOAD_CONCEPT_LIST_MODAL_ACCEPT";

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
  },
  parameters: any
}
