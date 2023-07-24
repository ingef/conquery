import styled from "@emotion/styled";

import {
  ColumnDescriptionSemanticConceptColumn,
  TimeStratifiedInfo,
} from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import WithTooltip from "../tooltip/WithTooltip";

import { ConceptBubble } from "./ConceptBubble";

const Container = styled("div")`
  display: grid;
  place-items: center;
  gap: 0 3px;
  padding: 10px;
`;

const BubbleYes = styled("div")`
  width: 10px;
  height: 10px;
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.blueGray};
`;
const BubbleNo = styled("div")`
  width: 10px;
  height: 10px;
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.grayLight};
`;

const Year = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

export const TimeStratifiedConceptChart = ({
  timeStratifiedInfo,
}: {
  timeStratifiedInfo: TimeStratifiedInfo;
}) => {
  const conceptColumn = timeStratifiedInfo.columns.at(-1);

  if (!conceptColumn) return null;

  const conceptSemantic = conceptColumn.semantics.find(
    (s): s is ColumnDescriptionSemanticConceptColumn =>
      s.type === "CONCEPT_COLUMN",
  );

  if (!conceptSemantic) return null;

  const descYearInfos = [...timeStratifiedInfo.years].sort(
    (a, b) => b.year - a.year,
  );

  const years = descYearInfos.map((y) => y.year);
  const valuesPerYear = descYearInfos.map((y) =>
    ((y.values[Object.keys(y.values)[0]] as string[]) || []).map(
      (conceptId) => getConceptById(conceptId, conceptSemantic?.concept)!,
    ),
  );

  const allValues = [
    ...new Set(
      valuesPerYear
        .flatMap((v) => v)
        .sort((a, b) => {
          const nA = Number(a?.label);
          const nB = Number(b?.label);
          if (!isNaN(nA) && !isNaN(nB)) return nA - nB;
          return a?.label.localeCompare(b?.label!);
        }),
    ),
  ];

  return (
    <Container
      style={{
        gridTemplateColumns: `repeat(${allValues.length + 1}, 1fr)`,
      }}
    >
      <div />
      {allValues.map((val) => (
        <WithTooltip key={val.label} text={val.description}>
          <ConceptBubble>{val.label}</ConceptBubble>
        </WithTooltip>
      ))}
      {years.map((year, i) => (
        <>
          <Year>{year}</Year>
          {allValues.map((val) =>
            valuesPerYear[i].includes(val) ? <BubbleYes /> : <BubbleNo />,
          )}
        </>
      ))}
    </Container>
  );
};
