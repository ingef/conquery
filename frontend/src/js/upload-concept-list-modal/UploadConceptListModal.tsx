import styled from "@emotion/styled";
import type { StateT } from "app-types";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { ConceptIdT, SelectOptionT } from "../api/types";
import PrimaryButton from "../button/PrimaryButton";
import type { TreesT } from "../concept-trees/reducer";
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import ScrollableList from "../scrollable-list/ScrollableList";
import InputPlain from "../ui-components/InputPlain";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { useSelectConceptRootNodeAndResolveCodes } from "./actions";
import { UploadConceptListModalStateT } from "./reducer";

const Root = styled("div")`
  padding: 0 0 10px;
`;

const Section = styled("div")`
  margin-top: 15px;
  padding: 15px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.1);
  display: grid;
  grid-gap: 20px;
`;

const Msg = styled("p")`
  margin: 0;
`;
const MsgRow = styled("div")`
  display: flex;
  align-items: flex-end;
`;

const BigIcon = styled(FaIcon)`
  font-size: 20px;
  margin-right: 10px;
`;
const ErrorIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.red};
`;
const SuccessIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const CenteredIcon = styled(FaIcon)`
  text-align: center;
`;
const SxPrimaryButton = styled(PrimaryButton)`
  margin-left: 15px;
  flex-shrink: 0;
`;

const selectUnresolvedItemsCount = (state: StateT) => {
  const { resolved } = state.uploadConceptListModal;

  return resolved && resolved.unknownCodes && resolved.unknownCodes.length
    ? resolved.unknownCodes.length
    : 0;
};

const selectResolvedItemsCount = (state: StateT) => {
  const { resolved } = state.uploadConceptListModal;

  return resolved &&
    resolved.resolvedConcepts &&
    resolved.resolvedConcepts.length
    ? resolved.resolvedConcepts.length
    : 0;
};

const selectAvailableConceptRootNodes = (state: StateT): SelectOptionT[] => {
  const { trees } = state.conceptTrees;

  if (!trees) return [];

  return Object.entries(trees)
    .map(([treeId, concept]) => ({ value: treeId, concept }))
    .filter(({ concept }) => concept.codeListResolvable)
    .sort((a, b) =>
      a.concept.label
        .toLowerCase()
        .localeCompare(b.concept.label.toLowerCase()),
    )
    .map(({ value, concept }) => ({ value, label: concept.label }));
};

interface PropsT {
  onAccept: (
    label: string,
    rootConcepts: TreesT,
    resolvedConcepts: ConceptIdT[],
  ) => void;
  onClose: () => void;
}

const UploadConceptListModal = ({ onAccept, onClose }: PropsT) => {
  const { t } = useTranslation();
  const {
    filename,
    conceptCodesFromFile,
    selectedConceptRootNode,
    loading,
    resolved,
    error,
  } = useSelector<StateT, UploadConceptListModalStateT>(
    (state) => state.uploadConceptListModal,
  );

  const availableConceptRootNodes = useSelector<StateT, SelectOptionT[]>(
    (state) => selectAvailableConceptRootNodes(state),
  );
  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const resolvedItemsCount = useSelector<StateT, number>((state) =>
    selectResolvedItemsCount(state),
  );
  const unresolvedItemsCount = useSelector<StateT, number>((state) =>
    selectUnresolvedItemsCount(state),
  );

  const [label, setLabel] = useState<string>(filename || "");

  useEffect(() => {
    if (filename) {
      setLabel(filename);
    }
  }, [filename]);

  const selectConceptRootNodeAndResolveCode =
    useSelectConceptRootNodeAndResolveCodes();

  if (!conceptCodesFromFile || conceptCodesFromFile.length === 0) {
    onClose();
  }

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (label && resolved.resolvedConcepts) {
      onAccept(label, rootConcepts, resolved.resolvedConcepts);
    }
    onClose();
  };

  return (
    <Modal
      closeIcon
      onClose={onClose}
      headline={t("uploadConceptListModal.headline")}
    >
      <Root>
        <InputSelect
          label={t("uploadConceptListModal.selectConceptRootNode")}
          value={
            availableConceptRootNodes.find(
              (node) => node.value === selectedConceptRootNode,
            ) || null
          }
          onChange={(value) =>
            selectConceptRootNodeAndResolveCode(
              (value?.value as string) || null,
              conceptCodesFromFile,
            )
          }
          options={availableConceptRootNodes}
        />
        {!!resolved && !hasResolvedItems && !hasUnresolvedItems && (
          <Section>
            <Msg>{t("uploadConceptListModal.nothingResolved")}</Msg>
          </Section>
        )}
        {(!!error ||
          !!loading ||
          (!!resolved && (hasResolvedItems || hasUnresolvedItems))) && (
          <Section>
            {error && (
              <p>
                <ErrorIcon icon="exclamation-circle" />
                {t("uploadConceptListModal.error")}
              </p>
            )}
            {loading && <CenteredIcon icon="spinner" />}
            {resolved && (
              <>
                {hasResolvedItems && (
                  <form onSubmit={onSubmit}>
                    <Msg>
                      <SuccessIcon icon="check-circle" />
                      {t("uploadConceptListModal.resolvedCodes", {
                        count: resolvedItemsCount,
                      })}
                    </Msg>
                    <MsgRow>
                      <InputPlain
                        label={t("uploadConceptListModal.label")}
                        fullWidth
                        inputProps={{
                          autoFocus: true,
                        }}
                        input={{
                          value: label,
                          onChange: setLabel,
                        }}
                      />
                      <SxPrimaryButton type="submit">
                        {t("uploadConceptListModal.insertNode")}
                      </SxPrimaryButton>
                    </MsgRow>
                  </form>
                )}
                {hasUnresolvedItems && (
                  <div>
                    <Msg>
                      <ErrorIcon icon="exclamation-circle" />
                      <span>
                        {t("uploadConceptListModal.unknownCodes", {
                          count: unresolvedItemsCount,
                        })}
                      </span>
                    </Msg>
                    <ScrollableList
                      maxVisibleItems={3}
                      fullWidth
                      items={resolved.unknownCodes || []}
                    />
                  </div>
                )}
              </>
            )}
          </Section>
        )}
      </Root>
    </Modal>
  );
};

export default UploadConceptListModal;
