import styled from "@emotion/styled";
import { FC, memo, useCallback, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { SecondaryId } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import { nodeIsConceptQueryNode } from "../model/node";
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

  useEffect(() => {
    const activeSecondaryIdNotFound =
      !!selectedSecondaryId &&
      (availableSecondaryIds.length === 0 ||
        !availableSecondaryIds
          .map((id) => id.id)
          .includes(selectedSecondaryId));

    if (activeSecondaryIdNotFound) {
      onSetSelectedSecondaryId(null);
    }
  }, [JSON.stringify(availableSecondaryIds), onSetSelectedSecondaryId]);

  const options = useMemo(
    () => [
      {
        value: "standard",
        label: t("queryEditor.secondaryIdStandard") as string,
      },
      ...availableSecondaryIds.map((id) => ({
        label: id.label,
        value: id.id,
        description: id.description,
      })),
    ],
    [JSON.stringify(availableSecondaryIds), t],
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
          <SxFaIcon active={!!value} left icon="microscope" />
          {t("queryEditor.secondaryId")}
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
