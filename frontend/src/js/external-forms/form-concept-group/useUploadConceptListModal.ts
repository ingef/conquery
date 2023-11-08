import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SelectOptionT } from "../../api/types";
import { StateT } from "../../app/reducers";
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
  TableConfig,
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
  tableConfig,
  isValidConcept,
}: {
  value: FormConceptGroupT[];
  onChange: (value: FormConceptGroupT[]) => void;
  newValue: FormConceptGroupT;
  defaults: ConceptListDefaultsType;
  tableConfig: TableConfig;
  isValidConcept?: (concept: DragItemConceptTreeNode) => boolean;
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  const initModal = async (file: File) => {
    const rows = await getUniqueFileRows(file);

    return dispatch(initUploadConceptListModal({ rows, filename: file.name }));
  };

  const [isOpen, setIsOpen] = useState(false);
  const [modalContext, setModalContext] =
    useState<UploadConceptListModalContext | null>(null);

  const onClose = useCallback(() => {
    setIsOpen(false);
    dispatch(resetUploadConceptListModal());
  }, [dispatch]);

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

  const onImportLines = (
    { lines, filename }: { lines: string[]; filename?: string },
    { valueIdx, conceptIdx }: UploadConceptListModalContext,
  ) => {
    setModalContext({ valueIdx, conceptIdx });
    dispatch(
      initUploadConceptListModal({
        rows: lines,
        filename: filename || t("importModal.pasted"),
      }),
    );

    setIsOpen(true); // For the Modal "container"
  };

  const onAcceptConceptsOrFilter = (
    label: string,
    resolvedConcepts: string[],
    resolvedFilter?: {
      tableId: string;
      filterId: string;
      value: SelectOptionT[];
    },
  ) => {
    if (!modalContext) return;
    const { valueIdx, conceptIdx } = modalContext;

    onChange(
      addConceptsFromFile(
        label,
        rootConcepts,
        resolvedConcepts,

        tableConfig,
        defaults,
        isValidConcept,

        value,
        newValue,

        valueIdx,
        conceptIdx,

        resolvedFilter,
      ),
    );

    onClose();
  };

  return {
    isOpen,
    onClose,
    onDropFile,
    onImportLines,
    onAcceptConceptsOrFilter,
  };
};
