import styled from "@emotion/styled";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { Fragment, memo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import {
  ColumnDescriptionSemanticConceptColumn,
  TimeStratifiedInfo,
} from "../../api/types";
import { StateT } from "../../app/reducers";
import { exists } from "../../common/helpers/exists";
import { getConceptById } from "../../concept-trees/globalTreeStoreHelper";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import { SmallHeading } from "./SmallHeading";
import { isConceptColumn, isMoneyColumn } from "./util";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 0 10px 0 0;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
  padding: 6px 10px;
  cursor: pointer;
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 8px 0;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid transparent;
  &:hover {
    border: 1px solid ${({ theme }) => theme.col.blueGray};
  }
`;

const Col = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

const Grid = styled("div")`
  display: grid;
  grid-template-columns: auto 45px;
  gap: 0px 10px;
`;

const ConceptRow = styled("div")`
  grid-column: span 2;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2px;
`;

const ConceptBubble = styled("span")`
  padding: 0 2px;
  border-radius: ${({ theme }) => theme.borderRadius};
  color: ${({ theme }) => theme.col.black};
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  font-size: ${({ theme }) => theme.font.tiny};
`;

const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 400;
  justify-self: end;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
  text-align: right;
`;

const Label = styled("div")`
  font-size: ${({ theme }) => theme.font.tiny};
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const TimeStratifiedInfos = ({
  year,
  timeStratifiedInfos,
}: {
  year: number;
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const currencyUnit = useSelector<StateT, string>(
    (state) => state.startup.config.currency.unit,
  );

  const infos = timeStratifiedInfos
    .map((info) => {
      return {
        info,
        yearInfo: info.years.find((i) => i.year === year),
      };
    })
    .filter(
      (
        i,
      ): i is {
        info: TimeStratifiedInfo;
        yearInfo: TimeStratifiedInfo["years"][number];
      } => !!i.yearInfo?.values && Object.entries(i.yearInfo.values).length > 0,
    );

  return (
    <Col>
      {infos.map(({ info, yearInfo }) => {
        return (
          <Grid key={info.label}>
            {Object.entries(yearInfo.values)
              .sort(
                ([l1], [l2]) =>
                  info.columns.findIndex((c) => c.label === l1) -
                  info.columns.findIndex((c) => c.label === l2),
              )
              .map(([label, value]) => {
                const column = info.columns.find((c) => c.label === label);

                if (!column) {
                  return null;
                }

                if (isConceptColumn(column)) {
                  const semantic = column.semantics.find(
                    (s): s is ColumnDescriptionSemanticConceptColumn =>
                      s.type === "CONCEPT_COLUMN",
                  );

                  if (value instanceof Array) {
                    const concepts = value
                      .map((v) =>
                        getConceptById(
                          // TODO: should be just v
                          semantic?.concept.split(".")[0] + "." + v,
                          semantic!.concept,
                        ),
                      )
                      .filter(exists);

                    return (
                      <Fragment key={label}>
                        <Label
                          style={{
                            gridColumn: "span 2",
                          }}
                        >
                          {label}
                        </Label>
                        <ConceptRow>
                          {concepts.map((concept) => (
                            <WithTooltip
                              key={concept.label}
                              text={concept.description}
                            >
                              <ConceptBubble>{concept.label}</ConceptBubble>
                            </WithTooltip>
                          ))}
                        </ConceptRow>
                      </Fragment>
                    );
                  }
                  // else if (typeof value === "string") {
                  //   const concept = getConceptById(semantic!.concept, value);

                  //   if (concept) valueFormatted = concept.label;
                  // }
                }

                let valueFormatted: string | number | string[] = value;
                if (typeof value === "number") {
                  valueFormatted = Math.round(value);
                } else if (value instanceof Array) {
                  valueFormatted = value.join(", ");
                }

                return (
                  <Fragment key={label}>
                    <Label>{label}</Label>
                    <Value title={String(valueFormatted)}>
                      {valueFormatted}
                      {isMoneyColumn(column) ? " " + currencyUnit : ""}
                    </Value>
                  </Fragment>
                );
              })}
          </Grid>
        );
      })}
    </Col>
  );
};

const YearHead = ({
  year,
  totalEvents,
  onClick,
  isOpen,
  timeStratifiedInfos,
}: {
  isOpen: boolean;
  year: number;
  totalEvents: number;
  onClick: () => void;
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const { t } = useTranslation();

  return (
    <Root>
      <StickyWrap onClick={onClick}>
        <FaIcon large gray icon={isOpen ? faCaretDown : faCaretRight} />
        <div>
          <SmallHeading>{year}</SmallHeading>
          <div>
            {totalEvents}&nbsp;{t("history.events", { count: totalEvents })}
          </div>
        </div>
        <span />
        <TimeStratifiedInfos
          year={year}
          timeStratifiedInfos={timeStratifiedInfos}
        />
      </StickyWrap>
    </Root>
  );
};

export default memo(YearHead);
