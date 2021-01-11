import { StateT } from "app-types";
import styled from "@emotion/styled";
import React, { FC, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";

import { exists } from "../common/helpers/exists";
import ToggleButton from "../form-components/ToggleButton";
import { T } from "../localization";

import { ConceptQueryNodeType, StandardQueryType } from "./types";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";
import { setSelectedSecondaryId } from "./actions";
import { SecondaryId } from "../api/types";
import FaIcon from "../icon/FaIcon";

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
  const query = useSelector<StateT, StandardQueryType>(
    (state) => state.queryEditor.query
  );

  const selectedSecondaryId = useSelector<StateT, SelectedSecondaryIdStateT>(
    (state) => state.queryEditor.selectedSecondaryId
  );
  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds
  );

  const dispatch = useDispatch();
  const onSetSelectedSecondaryId = (id: string | null) => {
    dispatch(setSelectedSecondaryId(id === "standard" ? null : id));
  };

  const availableSecondaryIds = Array.from(
    new Set(
      query.flatMap((group) =>
        group.elements
          .filter((el): el is ConceptQueryNodeType => !el.isPreviousQuery)
          .flatMap((el) =>
            el.tables
              .filter((table) => !table.exclude)
              .flatMap((table) => table.supportedSecondaryIds)
              .filter(exists)
          )
      )
    )
  )
    .map((id) => loadedSecondaryIds.find((secId) => secId.id === id))
    .filter(exists);

  useEffect(() => {
    if (
      !!selectedSecondaryId &&
      (availableSecondaryIds.length === 0 ||
        !availableSecondaryIds.map((id) => id.id).includes(selectedSecondaryId))
    ) {
      onSetSelectedSecondaryId(null);
    }
  }, [JSON.stringify(availableSecondaryIds)]);

  const options = [
    {
      value: "standard",
      label: T.translate("queryEditor.secondaryIdStandard") as string,
    },
    ...availableSecondaryIds.map((id) => ({
      label: id.label,
      value: id.id,
      description: id.description,
    })),
  ];

  if (options.length < 2) {
    return null;
  }

  return (
    <div>
      <Headline active={!!selectedSecondaryId}>
        <SxFaIcon active={!!selectedSecondaryId} left icon="microscope" />
        {T.translate("queryEditor.secondaryId")}
      </Headline>
      <ToggleButton
        input={{
          value: selectedSecondaryId || "standard",
          onChange: onSetSelectedSecondaryId,
        }}
        options={options}
      />
    </div>
  );
};

export default SecondaryIdSelector;
