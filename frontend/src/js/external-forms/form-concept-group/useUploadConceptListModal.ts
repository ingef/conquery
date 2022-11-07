import { useState } from "react";
import { useDispatch } from "react-redux";

import { getUniqueFileRows } from "../../common/helpers/fileHelper";
import { TreesT } from "../../concept-trees/reducer";
import { DragItemConceptTreeNode } from "../../standard-query-editor/types";
import {
  initUploadConceptListModal,
  resetUploadConceptListModal,
} from "../../upload-concept-list-modal/actions";
import type { ConceptListDefaults as ConceptListDefaultsType } from "../config-types";

import {
  addConceptsFromFile,
  FormConceptGroupT,
} from "./formConceptGroupState";

interface UploadConceptListModalContext {
  valueIdx: number;
  conceptIdx?: number;
}

export const useUploadConceptListModal = ({
  value,
  onChange,
  newValue,
  defaults,
  isValidConcept,
}: {
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  newValue: FormConceptGroupT;
  defaults: ConceptListDefaultsType;
  isValidConcept?: (concept: DragItemConceptTreeNode) => boolean;
}) => {
  const dispatch = useDispatch();
  const initModal = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    return dispatch(initUploadConceptListModal({ rows, filename: file.name }));
  };
  const resetModal = () => dispatch(resetUploadConceptListModal());

  const [isOpen, setIsOpen] = useState(false);
  const [modalContext, setModalContext] =
    useState<UploadConceptListModalContext | null>(null);

  const onClose = () => {
    setIsOpen(false); // For the Modal "container"
    resetModal(); // For the common UploadConceptListModal
  };

  const onDropFile = async (
    file: File,
    { valueIdx, conceptIdx }: UploadConceptListModalContext,
  ) => {
    setModalContext({ valueIdx, conceptIdx });

    // For the common UploadConceptListModal
    // Wait for file processing before opening the modal
    // => See QueryUploadConceptListModal actions
    await initModal(file);

    setIsOpen(true); // For the Modal "container"
  };

  const onAccept = (
    label: string,
    rootConcepts: TreesT,
    resolvedConcepts: string[],
  ) => {
    if (!modalContext) return;
    const { valueIdx, conceptIdx } = modalContext;

    onChange(
      addConceptsFromFile(
        label,
        rootConcepts,
        resolvedConcepts,

        defaults,
        isValidConcept,

        value,
        newValue,

        valueIdx,
        conceptIdx,
      ),
    );

    onClose();
  };

  return {
    isOpen,
    onClose,
    onDropFile,
    onAccept,
  };
};
