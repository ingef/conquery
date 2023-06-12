import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import { DateRangeT } from "../../api/types";
import { formatDate } from "../../common/helpers/dateHelper";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  font-family: monospace;
  display: inline-grid;
  gap: 0 5px;
  grid-template-columns: auto 1fr;
`;

const Label = styled("div")`
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  justify-self: flex-end;
`;

const getFormattedDate = (date: string | undefined, dateFormat: string) => {
  if (!date) return null;

  const d = new Date(date);

  if (isNaN(d.getTime())) return null;

  return formatDate(d, dateFormat);
};

export const DateRange = ({ dateRange }: { dateRange: DateRangeT }) => {
  const { t } = useTranslation();
  const dateFormat = t("inputDateRange.dateFormat");

  const dateMin = getFormattedDate(dateRange.min, dateFormat);
  const dateMax = getFormattedDate(dateRange.max, dateFormat);

  return (
    <Root>
      {dateMin && (
        <>
          <Label>{t("inputDateRange.from")}</Label>
          <span>{dateMin}</span>
        </>
      )}
      {dateMax && dateMax !== dateMin && (
        <>
          <Label>{t("inputDateRange.to")}</Label>
          <span>{dateMax}</span>
        </>
      )}
    </Root>
  );
};
