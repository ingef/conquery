import { StateT } from "app-types";
import { exists } from "../common/helpers/exists";
import React, { FC, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { ConceptQueryNodeType, StandardQueryType } from "./types";
import ToggleButton from "js/form-components/ToggleButton";
import styled from "app-theme";

const Headline = styled.h3`
  font-size: ${({ theme }) => theme.font.md};
`;

const SecondaryIdSelector: FC = () => {
  const [selected, setSelected] = useState(null);

  const query = useSelector<StateT, StandardQueryType>(
    (state) => state.queryEditor.query
  );

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
      (!selected || !availableSecondaryIds.includes(selected))
    ) {
      setSelected(availableSecondaryIds[0]);
    }
  }, [JSON.stringify(availableSecondaryIds)]);

  return (
    <div>
      <Headline>Analyse-Ebene</Headline>
      <ToggleButton
        input={{
          value: selected,
          onChange: setSelected,
        }}
        options={availableSecondaryIds.map((id) => ({ label: id, value: id }))}
      />
    </div>
  );
};

export default SecondaryIdSelector;
