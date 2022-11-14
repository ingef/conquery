import styled from "@emotion/styled";
import { FormEvent, useState, useEffect, useMemo, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type {
  ConceptIdT,
  ConceptT,
  FilterT,
  PostConceptResolveResponseT,
  SelectOptionT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import PrimaryButton from "../button/PrimaryButton";
import type { TreesT } from "../concept-trees/reducer";
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import { nodeIsElement } from "../model/node";
import ScrollableList from "../scrollable-list/ScrollableList";
import InputPlain from "../ui-components/InputPlain/InputPlain";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { useResolveCodes } from "./actions";
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
const SxInputSelect = styled(InputSelect)`
  width: 500px;
`;

const useUnresolvedItemsCount = () => {
  const resolved = useSelector<StateT, PostConceptResolveResponseT>(
    (state) => state.uploadConceptListModal.resolved,
  );

  return useMemo(
    () =>
      resolved && resolved.unknownCodes && resolved.unknownCodes.length > 0
        ? resolved.unknownCodes.length
        : 0,
    [resolved],
  );
};

const useResolvedItemsCount = () => {
  const resolved = useSelector<StateT, PostConceptResolveResponseT>(
    (state) => state.uploadConceptListModal.resolved,
  );

  return useMemo(
    () =>
      resolved && resolved.resolvedConcepts && resolved.resolvedConcepts.length
        ? resolved.resolvedConcepts.length
        : 0,
    [resolved],
  );
};

const getCombinedLabel = (concept: ConceptT, filter: FilterT) => {
  if (concept.label === filter.label) return concept.label;

  return `${concept.label} – ${filter.label}`;
};

const useDropdownOptions = () => {
  const trees = useSelector((state: StateT) => state.conceptTrees.trees);

  const conceptRootNodes = useMemo(
    () =>
      Object.entries(trees)
        .filter(([, concept]) => concept.codeListResolvable)
        .map(([treeId, concept]) => ({
          id: treeId,
          type: "concept" as const,
          concept,
        })),
    [trees],
  );

  const allBigMultiSelectFilters = useMemo(() => {
    return Object.values(trees)
      .filter(nodeIsElement)
      .flatMap(
        (concept) =>
          concept.tables
            ?.flatMap((t) => t.filters)
            .filter(
              (filter) =>
                filter.type === "BIG_MULTI_SELECT" && filter.allowDropFile,
            )
            .map((filter) => {
              return {
                id: filter.id,
                type: "filter" as const,
                concept,
                filter,
              };
            }) || [],
      );
  }, [trees]);

  const allOptions = useMemo(
    () => [...conceptRootNodes, ...allBigMultiSelectFilters],
    [conceptRootNodes, allBigMultiSelectFilters],
  );

  const selectOptionsDetails = useMemo(
    () => Object.fromEntries(allOptions.map((opt) => [opt.id, opt])),
    [allOptions],
  );

  const selectOptions = useMemo(
    () =>
      allOptions
        .map((opt) => {
          if (opt.type === "concept") {
            return { value: opt.id, label: opt.concept.label };
          } else {
            return {
              value: opt.id,
              label: getCombinedLabel(opt.concept, opt.filter),
            };
          }
        })
        .sort((a, b) =>
          a.label.toLowerCase().localeCompare(b.label.toLowerCase()),
        ),
    [allOptions],
  );

  return {
    selectOptions,
    selectOptionsDetails, // Used to look up details about the selected option
  };
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
  const { filename, conceptCodesFromFile, loading, resolved, error } =
    useSelector<StateT, UploadConceptListModalStateT>(
      (state) => state.uploadConceptListModal,
    );

  const { selectOptions, selectOptionsDetails } = useDropdownOptions();

  const [selectedValue, setSelectedValue] = useState<string | null>(null);

  const rootConcepts = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const resolvedItemsCount = useResolvedItemsCount();
  const unresolvedItemsCount = useUnresolvedItemsCount();

  const [label, setLabel] = useState<string>(filename || "");

  useEffect(() => {
    if (filename) {
      setLabel(filename);
    }
  }, [filename]);

  const resolveCodes = useResolveCodes();

  if (!conceptCodesFromFile || conceptCodesFromFile.length === 0) {
    onClose();
  }

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  const onSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (label && resolved.resolvedConcepts) {
      onAccept(label, rootConcepts, resolved.resolvedConcepts);
    }
    onClose();
  };

  const onChange = useCallback(
    (opt: SelectOptionT | null) => {
      if (!opt) {
        setSelectedValue(null);
        return;
      }

      setSelectedValue(opt.value as string);

      const optionDetails = selectOptionsDetails[opt.value];

      if (optionDetails.type === "concept") {
        resolveCodes(opt.value as string, conceptCodesFromFile);
      } else {
        console.log(optionDetails);
      }
    },
    [conceptCodesFromFile, resolveCodes, selectOptionsDetails],
  );

  return (
    <Modal
      closeIcon
      onClose={onClose}
      headline={t("uploadConceptListModal.headline")}
    >
      <Root>
        <SxInputSelect
          label={t("uploadConceptListModal.selectConceptRootNode")}
          value={
            selectOptions.find(({ value }) => value === selectedValue) || null
          }
          onChange={onChange}
          options={selectOptions}
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
                        value={label}
                        onChange={(value) => setLabel(value as string)}
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
