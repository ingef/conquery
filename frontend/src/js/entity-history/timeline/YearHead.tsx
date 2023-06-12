import styled from "@emotion/styled";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { Fragment, memo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { TimeStratifiedInfo } from "../../api/types";
import { StateT } from "../../app/reducers";
import FaIcon from "../../icon/FaIcon";

import { SmallHeading } from "./SmallHeading";
import { getColumnType } from "./util";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 0 10px 0 0;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
  padding: 5px;
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
                const columnType = getColumnType(info, label);
                const valueFormatted =
                  typeof value === "number"
                    ? Math.round(value)
                    : value instanceof Array
                    ? value.join(", ")
                    : value;

                return (
                  <Fragment key={label}>
                    <Label>{label}</Label>
                    <Value>
                      {valueFormatted}
                      {columnType === "MONEY" ? " " + currencyUnit : ""}
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
