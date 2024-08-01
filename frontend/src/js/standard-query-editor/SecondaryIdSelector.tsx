import styled from "@emotion/styled";
import { faMicroscope } from "@fortawesome/free-solid-svg-icons";
import { FC, memo, useCallback, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SecondaryId } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import { nodeIsConceptQueryNode } from "../model/node";
import InfoTooltip from "../tooltip/InfoTooltip";
import ToggleButton from "../ui-components/ToggleButton";

import { setSelectedSecondaryId } from "./actions";
import type { StandardQueryStateT } from "./queryReducer";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";

const Headline = styled.h3<{ active?: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 0;
  text-transform: uppercase;
  transition: color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
`;

const SxFaIcon = styled(FaIcon)<{ active?: boolean }>`
  transition: color ${({ theme }) => theme.transitionTime};
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
`;

const SecondaryIdSelector: FC = () => {
  const { t } = useTranslation();
  const query = useSelector<StateT, StandardQueryStateT>(
    (state) => state.queryEditor.query,
  );
  const selectedSecondaryId = useSelector<StateT, SelectedSecondaryIdStateT>(
    (state) => state.queryEditor.selectedSecondaryId,
  );
  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );
  const dispatch = useDispatch();

  const onSetSelectedSecondaryId = useCallback(
    (id: string | null) => {
      dispatch(
        setSelectedSecondaryId({ secondaryId: id === "standard" ? null : id }),
      );
    },
    [dispatch],
  );

  // The following is slightly complicated memoization.
  // The reason: `query` is changing frequently, e.g. with every filter change in every table.
  // but most of the changes likely won't affect the availableSecondaryIds.
  // So we only want to trigger rerenders of the selector UI
  // when the `availableSecondaryId` actually change, i.e. when a secondary id
  // is added or removed due to a change in the query,
  // e.g. when certain concepts or queries are added or removed
  const availableSecondaryIds = useMemo(
    () =>
      Array.from(
        new Set(
          query.flatMap((group) =>
            group.elements.flatMap((el) => {
              if (nodeIsConceptQueryNode(el)) {
                return el.tables
                  .filter((table) => !table.exclude)
                  .flatMap((table) => table.supportedSecondaryIds)
                  .filter(exists);
              } else {
                return el.availableSecondaryIds || [];
              }
            }),
          ),
        ),
      )
        .map((id) => loadedSecondaryIds.find((secId) => secId.id === id))
        .filter(exists),
    [query, loadedSecondaryIds],
  );

  const availableSecondaryIdsRef = useRef(availableSecondaryIds);
  availableSecondaryIdsRef.current = availableSecondaryIds;

  const availableSecondaryIdsString = JSON.stringify(availableSecondaryIds);

  useEffect(
    function unselectSecondaryId() {
      const activeSecondaryIdNotFound =
        !!selectedSecondaryId &&
        (availableSecondaryIdsRef.current.length === 0 ||
          !availableSecondaryIdsRef.current
            .map((id) => id.id)
            .includes(selectedSecondaryId));

      if (activeSecondaryIdNotFound) {
        onSetSelectedSecondaryId(null);
      }
    },
    [
      availableSecondaryIdsString,
      onSetSelectedSecondaryId,
      selectedSecondaryId,
    ],
  );

  const options = useMemo(
    () => [
      {
        value: "standard",
        label: t("queryEditor.secondaryIdStandard") as string,
      },
      ...availableSecondaryIdsRef.current.map((id) => ({
        label: id.label,
        value: id.id,
        description: id.description,
      })),
    ],
    // We DO want to recompute this when the availableSecondaryIds change,
    // see explanation above
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [availableSecondaryIdsString, t],
  );

  if (options.length < 2) {
    return null;
  }

  return (
    <SecondaryIdSelectorUI
      options={options}
      value={selectedSecondaryId}
      onChange={onSetSelectedSecondaryId}
    />
  );
};

const SecondaryIdSelectorUI = memo(
  ({
    options,
    value,
    onChange,
  }: {
    options: { label: string; value: string }[];
    value: string | null;
    onChange: (value: string) => void;
  }) => {
    const { t } = useTranslation();

    return (
      <div>
        <Headline active={!!value}>
          <SxFaIcon active={!!value} left icon={faMicroscope} />
          {t("queryEditor.secondaryId")}
          <InfoTooltip text={t("queryEditor.secondaryIdTooltip")} />
        </Headline>
        <ToggleButton
          value={value || "standard"}
          onChange={onChange}
          options={options}
        />
      </div>
    );
  },
);

export default memo(SecondaryIdSelector);
