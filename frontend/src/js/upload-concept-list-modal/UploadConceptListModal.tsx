import styled from "@emotion/styled";
import {
  faCheckCircle,
  faExclamationCircle,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import {
  FormEvent,
  memo,
  useCallback,
  useEffect,
  useMemo,
  useState,
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
import FaIcon from "../icon/FaIcon";
import Modal from "../modal/Modal";
import { nodeIsElement } from "../model/node";
import ScrollableList from "../scrollable-list/ScrollableList";
import InputPlain from "../ui-components/InputPlain/InputPlain";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { DropdownOption } from "./DropdownOption";
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
const Row = styled("div")`
  display: flex;
  align-items: center;
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
  width: 60vw;
  max-width: 900px;
`;

const useUnresolvedItemsCount = (
  resolvedConcepts: PostConceptResolveResponseT | null,
  resolvedFilters: PostFilterResolveResponseT | null,
) => {
  return useMemo(() => {
    const concepts = resolvedConcepts?.unknownCodes?.length || 0;
    const filters = resolvedFilters?.unknownCodes?.length || 0;

    return concepts + filters;
  }, [resolvedConcepts, resolvedFilters]);
};

const useResolvedItemsCount = (
  resolvedConcepts: PostConceptResolveResponseT | null,
  resolvedFilters: PostFilterResolveResponseT | null,
) => {
  return useMemo(() => {
    const concepts = resolvedConcepts?.resolvedConcepts?.length || 0;
    const filters = resolvedFilters?.resolvedFilter?.value?.length || 0;

    return concepts + filters;
  }, [resolvedConcepts, resolvedFilters]);
};

const getCombinedLabel = (concept: ConceptT, filter: FilterT) => {
  if (concept.label === filter.label) return concept.label;

  return `${concept.label} | ${filter.label.trim()}`;
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
              .map((filter, idx) => ({ filter, idx }))
              .filter(
                ({ filter }) =>
                  filter.type === "BIG_MULTI_SELECT" && filter.allowDropFile,
              )
              .map(({ filter, idx }) => {
                return {
                  id: filter.id,
                  type: "filter" as const,
                  conceptId,
                  concept,
                  tableId: table.id,
                  filter,
                  filterIdx: idx + 1,
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
            return {
              value: opt.id,
              label: opt.concept.label,
              displayLabel: <DropdownOption conceptLabel={opt.concept.label} />,
            };
          } else {
            const label = getCombinedLabel(opt.concept, opt.filter);
            const details = selectOptionsDetails[opt.id];
            const filterIdx =
              details.type === "filter" ? details.filterIdx : undefined;

            return {
              value: opt.id,
              label,
              displayLabel: (
                <DropdownOption
                  conceptLabel={opt.concept.label}
                  filterLabel={opt.filter.label}
                  filterIdx={filterIdx}
                />
              ),
            };
          }
        })
        .sort((a, b) =>
          a.label.toLowerCase().localeCompare(b.label.toLowerCase()),
        ),
    [allOptions, selectOptionsDetails],
  );

  return {
    selectOptions,
    selectOptionsDetails, // Used to look up details about the selected option
  };
};

export const useResolveConcepts = () => {
  const postConceptsListToResolve = usePostConceptsListToResolve();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [resolved, setResolved] = useState<PostConceptResolveResponseT | null>(
    null,
  );

  const onResolve = useCallback(
    async (treeId: string, conceptCodes: string[]) => {
      setLoading(true);
      try {
        const results = await postConceptsListToResolve(treeId, conceptCodes);
        setResolved(results);
      } catch (e) {
        setError(e as Error);
      }
      setLoading(false);
    },
    [postConceptsListToResolve],
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
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [resolved, setResolved] = useState<PostFilterResolveResponseT | null>(
    null,
  );
  const postFilterValuesResolve = usePostFilterValuesResolve();

  const onResolve = useCallback(
    async (filterId: string, values: string[]) => {
      setLoading(true);
      try {
        const results = await postFilterValuesResolve(filterId, values);

        setResolved(results);
      } catch (e) {
        setError(e as Error);
      }
      setLoading(false);
    },
    [postFilterValuesResolve],
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
  onAcceptConceptsOrFilter,
}: {
  onClose: () => void;
  onAcceptConceptsOrFilter: (
    label: string,
    resolvedConcepts: ConceptIdT[],
    resolvedFilter?: {
      tableId: string;
      filterId: string;
      value: SelectOptionT[];
    },
  ) => void;
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

      // MAYBE ACCEPT CONCEPTS
      if (label && resolvedConcepts?.resolvedConcepts) {
        onAcceptConceptsOrFilter(label, resolvedConcepts.resolvedConcepts);
        onClose();
        return;
      }

      // MAYBE ACCEPT FILTERS
      if (!selectedValue) return;

      const optionDetails = selectOptionsDetails[selectedValue];

      if (
        label &&
        resolvedFilters?.resolvedFilter &&
        optionDetails.type === "filter"
      ) {
        onAcceptConceptsOrFilter(label, [optionDetails.conceptId], {
          tableId: optionDetails.tableId,
          filterId: optionDetails.id,
          value: resolvedFilters.resolvedFilter.value,
        });
        onClose();
      }
    },
    [
      label,
      selectOptionsDetails,
      selectedValue,
      resolvedConcepts,
      resolvedFilters,
      onClose,
      onAcceptConceptsOrFilter,
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
        onResolveFilters(id, fileRows);
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

  // Pretty custom sorting logic, tested with many combinations of
  // filters and concepts to "work well".
  // Tries to
  // - keep concept and its filters together (in asc order of the filterIdx)
  // - tries to prioritize concepts that are matched exactly by search query
  // But less "set in stone" than it looks
  const sortOptions = useCallback(
    (a: SelectOptionT, b: SelectOptionT, query: string) => {
      const aDetails = selectOptionsDetails[a.value as string];
      const bDetails = selectOptionsDetails[b.value as string];

      if (aDetails.type === "concept" && bDetails.type === "concept") {
        return a.label.localeCompare(b.label);
      }

      if (aDetails.type === "filter" && bDetails.type === "filter") {
        const sameConcept = aDetails.conceptId === bDetails.conceptId;

        if (sameConcept) {
          return aDetails.filterIdx - bDetails.filterIdx;
        }
      }

      if (aDetails.concept.label === bDetails.concept.label) {
        return aDetails.type === "concept" ? -1 : 1;
      }

      const aConceptLabel = aDetails.concept.label.toLowerCase();
      const bConceptLabel = bDetails.concept.label.toLowerCase();
      const queryLower = query.toLowerCase();

      const aConceptLabelEqual = aConceptLabel === queryLower;
      const bConceptLabelEqual = bConceptLabel === queryLower;

      if (aConceptLabelEqual) {
        return -1;
      } else if (bConceptLabelEqual) {
        return 1;
      }

      return aDetails.concept.label.localeCompare(bDetails.concept.label);
    },
    [selectOptionsDetails],
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
          sortOptions={sortOptions}
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
              <Row>
                <ErrorIcon icon={faExclamationCircle} />
                {t("uploadConceptListModal.error")}
              </Row>
            )}
            {loading && <CenteredIcon icon={faSpinner} />}
            {(!!resolvedConcepts || !!resolvedFilters) && (
              <>
                {hasResolvedItems && (
                  <form onSubmit={onSubmit}>
                    <Msg>
                      <SuccessIcon icon={faCheckCircle} />
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
                      <ErrorIcon icon={faExclamationCircle} />
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
