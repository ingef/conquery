import styled from "@emotion/styled";
import { faBan } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import {
  ColumnDescriptionSemanticConceptColumn,
  TimeStratifiedInfo,
} from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

import { ConceptBubble } from "./ConceptBubble";

const Container = styled("div")`
  display: grid;
  place-items: center;
  max-width: 100%;
  overflow-x: auto;
  gap: 0 3px;
  padding: 10px;
`;

const EmptyMsg = styled("p")`
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.gray};
  margin: 40px 0;
  display: flex;
  align-items: center;
  gap: 10px;
`;

const BubbleYes = styled("div")`
  width: 14px;
  height: 14px;
  background-color: ${({ theme }) => theme.col.blueGray};
`;
const BubbleNo = styled("div")`
  width: 14px;
  height: 14px;
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
  const { t } = useTranslation();
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

  if (allValues.length === 0)
    return (
      <Container>
        <EmptyMsg>
          <FaIcon gray icon={faBan} />
          {t("history.noData")}
        </EmptyMsg>
      </Container>
    );

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
