import styled from "@emotion/styled";
import {
  FormEvent,
  useState,
  useEffect,
  useMemo,
  useCallback,
  memo,
} from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import {
  usePostConceptsListToResolve,
  usePostFilterValuesResolve,
} from "../api/api";
import type {
  ConceptElementT,
  ConceptIdT,
  ConceptT,
  FilterT,
  PostConceptResolveResponseT,
  PostFilterResolveResponseT,
  SelectOptionT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import PrimaryButton from "../button/PrimaryButton";
import { useDatasetId } from "../dataset/selectors";
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import { nodeIsElement } from "../model/node";
import ScrollableList from "../scrollable-list/ScrollableList";
import InputPlain from "../ui-components/InputPlain/InputPlain";
import InputSelect from "../ui-components/InputSelect/InputSelect";

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

const useUnresolvedItemsCount = (
  resolvedConcepts: PostConceptResolveResponseT | null,
  resolvedFilters: PostFilterResolveResponseT | null,
) => {
  const conceptsCount = useMemo(
    () => resolvedConcepts?.unknownCodes?.length || 0,
    [resolvedConcepts],
  );

  const filtersCount = useMemo(
    () => resolvedFilters?.unknownCodes?.length || 0,
    [resolvedFilters],
  );

  return conceptsCount + filtersCount;
};

const useResolvedItemsCount = (
  resolvedConcepts: PostConceptResolveResponseT | null,
  resolvedFilters: PostFilterResolveResponseT | null,
) => {
  const conceptsCount = useMemo(
    () => resolvedConcepts?.resolvedConcepts?.length || 0,
    [resolvedConcepts],
  );

  const filtersCount = useMemo(
    () => resolvedFilters?.resolvedFilter?.value?.length || 0,
    [resolvedFilters],
  );

  return conceptsCount + filtersCount;
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
    return Object.entries(trees)
      .filter((entry): entry is [id: string, concept: ConceptElementT] =>
        nodeIsElement(entry[1]),
      )
      .flatMap(
        ([conceptId, concept]) =>
          concept.tables?.flatMap((table) =>
            table.filters
              .filter(
                (filter) =>
                  filter.type === "BIG_MULTI_SELECT" && filter.allowDropFile,
              )
              .map((filter) => {
                return {
                  id: filter.id,
                  type: "filter" as const,
                  conceptId,
                  concept,
                  tableId: table.id,
                  filter,
                };
              }),
          ) || [],
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

export const useResolveConcepts = () => {
  const postConceptsListToResolve = usePostConceptsListToResolve();
  const datasetId = useDatasetId();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [resolved, setResolved] = useState<PostConceptResolveResponseT | null>(
    null,
  );

  const onResolve = useCallback(
    async (treeId: string, conceptCodes: string[]) => {
      if (!datasetId) {
        return;
      }

      setLoading(true);
      try {
        const results = await postConceptsListToResolve(
          datasetId,
          treeId,
          conceptCodes,
        );
        setResolved(results);
      } catch (e) {
        setError(e as Error);
      }
      setLoading(false);
    },
    [datasetId, postConceptsListToResolve],
  );

  const onReset = useCallback(() => {
    setResolved(null);
    setError(null);
  }, []);

  return {
    loading,
    error,
    resolved,
    onResolve,
    onReset,
  };
};

const useResolveFilterValues = () => {
  const datasetId = useDatasetId();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [resolved, setResolved] = useState<PostFilterResolveResponseT | null>(
    null,
  );
  const postFilterValuesResolve = usePostFilterValuesResolve();

  const onResolve = useCallback(
    async (
      conceptId: string,
      tableId: string,
      filterId: string,
      values: string[],
    ) => {
      if (!datasetId) return;

      setLoading(true);
      try {
        const results = await postFilterValuesResolve(
          datasetId,
          conceptId,
          tableId,
          filterId,
          values,
        );

        setResolved(results);
      } catch (e) {
        setError(e as Error);
      }
      setLoading(false);
    },
    [datasetId, postFilterValuesResolve],
  );

  const onReset = useCallback(() => {
    setResolved(null);
    setError(null);
  }, []);

  return {
    loading,
    error,
    resolved,
    onResolve,
    onReset,
  };
};

const useResolveConceptsAndFilterValues = () => {
  const {
    resolved: resolvedConcepts,
    loading: conceptsLoading,
    error: conceptsError,
    onResolve: onResolveConcepts,
    onReset: onResetConcepts,
  } = useResolveConcepts();
  const {
    resolved: resolvedFilters,
    loading: filterLoading,
    error: filterError,
    onResolve: onResolveFilters,
    onReset: onResetFilters,
  } = useResolveFilterValues();

  const onReset = useCallback(() => {
    onResetConcepts();
    onResetFilters();
  }, [onResetConcepts, onResetFilters]);

  return {
    resolvedConcepts,
    resolvedFilters,
    loading: conceptsLoading || filterLoading,
    error: conceptsError || filterError,
    onResolveConcepts,
    onResolveFilters,
    onReset,
  };
};

const UploadConceptListModal = ({
  onClose,
  onAcceptConcepts,
  onAcceptFilters,
}: {
  onClose: () => void;
  onAcceptConcepts: (label: string, resolvedConcepts: ConceptIdT[]) => void;
  onAcceptFilters: () => void;
}) => {
  const { t } = useTranslation();
  const { filename, fileRows } = useSelector<
    StateT,
    UploadConceptListModalStateT
  >((state) => state.uploadConceptListModal);

  const { selectOptions, selectOptionsDetails } = useDropdownOptions();

  const [selectedValue, setSelectedValue] = useState<string | null>(null);

  const {
    resolvedConcepts,
    resolvedFilters,
    loading,
    error,
    onResolveConcepts,
    onResolveFilters,
    onReset,
  } = useResolveConceptsAndFilterValues();

  const resolvedItemsCount = useResolvedItemsCount(
    resolvedConcepts,
    resolvedFilters,
  );
  const unresolvedItemsCount = useUnresolvedItemsCount(
    resolvedConcepts,
    resolvedFilters,
  );

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  const [label, setLabel] = useState<string>(filename || "");

  useEffect(() => {
    if (filename) {
      setLabel(filename);
    }
  }, [filename]);

  if (!fileRows || fileRows.length === 0) {
    onClose();
  }

  const onSubmit = useCallback(
    (e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();

      if (label && resolvedConcepts?.resolvedConcepts) {
        onAcceptConcepts(label, resolvedConcepts.resolvedConcepts);
      }

      if (label && resolvedFilters?.resolvedFilter) {
        onAcceptFilters();
      }

      onClose();
    },
    [
      label,
      resolvedConcepts,
      resolvedFilters,
      onClose,
      onAcceptConcepts,
      onAcceptFilters,
    ],
  );

  const onChange = useCallback(
    (opt: SelectOptionT | null) => {
      if (!opt) {
        setSelectedValue(null);
        return;
      }

      // Either a conceptId or a filterId
      const id = opt.value as string;

      setSelectedValue(id);
      onReset();

      const optionDetails = selectOptionsDetails[id];

      if (optionDetails.type === "concept") {
        onResolveConcepts(id, fileRows);
      } else {
        onResolveFilters(
          optionDetails.conceptId,
          optionDetails.tableId,
          id,
          fileRows,
        );
      }
    },
    [
      onReset,
      selectOptionsDetails,
      onResolveConcepts,
      fileRows,
      onResolveFilters,
    ],
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
        {(!!resolvedFilters || !!resolvedConcepts) &&
          !hasResolvedItems &&
          !hasUnresolvedItems && (
            <Section>
              <Msg>{t("uploadConceptListModal.nothingResolved")}</Msg>
            </Section>
          )}
        {(!!error ||
          !!loading ||
          ((!!resolvedConcepts || !!resolvedFilters) &&
            (hasResolvedItems || hasUnresolvedItems))) && (
          <Section>
            {error && (
              <p>
                <ErrorIcon icon="exclamation-circle" />
                {t("uploadConceptListModal.error")}
              </p>
            )}
            {loading && <CenteredIcon icon="spinner" />}
            {(!!resolvedConcepts || !!resolvedFilters) && (
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
                      items={
                        resolvedFilters?.unknownCodes ||
                        resolvedConcepts?.unknownCodes ||
                        []
                      }
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

export default memo(UploadConceptListModal);
