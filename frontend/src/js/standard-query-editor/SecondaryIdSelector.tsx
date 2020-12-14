import { StateT } from "app-types";
import styled from "app-theme";
import React, { FC, useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";

import { exists } from "../common/helpers/exists";
import ToggleButton from "../form-components/ToggleButton";
import { T } from "../localization";

import { ConceptQueryNodeType, StandardQueryType } from "./types";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";
import { setSelectedSecondaryId } from "./actions";

const Headline = styled.h3`
  font-size: ${({ theme }) => theme.font.md};
`;

const SecondaryIdSelector: FC = () => {
  const query = useSelector<StateT, StandardQueryType>(
    (state) => state.queryEditor.query
  );

  const selectedSecondaryId = useSelector<StateT, SelectedSecondaryIdStateT>(
    (state) => state.queryEditor.selectedSecondaryId
  );

  const dispatch = useDispatch();
  const onSetSelectedSecondaryId = (id: string) =>
    dispatch(setSelectedSecondaryId(id));

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
  );

  useEffect(() => {
    if (
      availableSecondaryIds.length > 0 &&
      (!selectedSecondaryId ||
        !availableSecondaryIds.includes(selectedSecondaryId))
    ) {
      onSetSelectedSecondaryId(availableSecondaryIds[0]);
    }
  }, [JSON.stringify(availableSecondaryIds)]);

  return (
    <div>
      <Headline>{T.translate("queryEditor.secondaryId")}</Headline>
      <ToggleButton
        input={{
          value: selectedSecondaryId,
          onChange: onSetSelectedSecondaryId,
        }}
        options={availableSecondaryIds.map((id) => ({ label: id, value: id }))}
      />
    </div>
  );
};

export default SecondaryIdSelector;
