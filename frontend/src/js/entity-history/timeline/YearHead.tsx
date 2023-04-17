import styled from "@emotion/styled";
import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { TimeStratifiedInfo } from "../../api/types";
import { StateT } from "../../app/reducers";
import { exists } from "../../common/helpers/exists";
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

const Grid = styled("div")`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 2px 10px;
`;

const Value = styled("div")`
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 400;
  white-space: nowrap;
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
    (state) => state.startup.config.currency.prefix,
  );
  const yearInfos = timeStratifiedInfos
    .map((i) => i.years.find((info) => info.year === year))
    .filter(exists);

  return (
    <>
      <span />{" "}
      {yearInfos.map((info) => (
        <Grid>
          {Object.entries(info.values).map(([label, value]) => {
            const columnType = getColumnType(timeStratifiedInfos[0], label);

            return (
              <>
                <Value>
                  {value}
                  {columnType === "MONEY" ? " " + currencyUnit : ""}
                </Value>
                <Label>{label}</Label>
              </>
            );
          })}
        </Grid>
      ))}
    </>
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
        <TimeStratifiedInfos
          year={year}
          timeStratifiedInfos={timeStratifiedInfos}
        />
      </StickyWrap>
    </Root>
  );
};

export default memo(YearHead);
