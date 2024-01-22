import styled from "@emotion/styled";
import { faChevronLeft } from "@fortawesome/free-solid-svg-icons";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import {
  formatStdDate,
  useFormatDateDistance,
} from "../common/helpers/dateHelper";

import ColumnStats from "./ColumnStats";
import type { ColumnDescriptionType } from "./Preview";
import { StatsHeadline } from "./StatsHeadline";
import StatsSubline from "./StatsSubline";

const TopRow = styled("div")`
  margin: 12px 0 20px;
  width: 100%;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
`;

const StdRow = styled("div")`
  display: flex;
  align-items: center;
`;

const Stat = styled("code")`
  display: block;
  margin: 0;
  padding-right: 10px;
  font-size: ${({ theme }) => theme.font.xs};
`;

const BStat = styled(Stat)`
  font-weight: 700;
`;

const Headline = styled("h2")`
  font-size: ${({ theme }) => theme.font.md};
  margin: 0;
`;

const HeadInfo = styled("div")`
  margin: 0 20px;
`;

const Tr = styled("tr")`
  line-height: 1;
`;

const SxIconButton = styled(IconButton)`
  background-color: white;
`;

const StatsCard = styled("div")`
  padding: 10px;
  box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.2);
  background-color: white;
`;
const StatsContainer = styled("div")`
  display: grid;
  grid-template-columns: minmax(120px, 200px);
  grid-template-rows: 1fr auto;
  grid-auto-flow: column;
  gap: 5px 20px;
  overflow-x: auto;
`;

const COLUMN_TYPES_WITH_SUPPORTED_STATS = new Set<ColumnDescriptionType>([
  "MONEY",
  "NUMERIC",
  "INTEGER",
]);

interface PropsT {
  columns: ColumnDescriptionType[];
  rawPreviewData: string[][];
  onClose: () => void;
  minDate: Date | null;
  maxDate: Date | null;
}

const PreviewInfo: FC<PropsT> = ({
  rawPreviewData,
  columns,
  onClose,
  minDate,
  maxDate,
}) => {
  const { t } = useTranslation();
  const formatDateDistance = useFormatDateDistance();

  if (rawPreviewData.length < 2) return null;

  const showStats = columns.some((column) =>
    COLUMN_TYPES_WITH_SUPPORTED_STATS.has(column),
  );

  return (
    <div>
      <TopRow>
        <div>
          <StdRow>
            <SxIconButton frame icon={faChevronLeft} onClick={onClose}>
              {t("common.back")}
            </SxIconButton>
            <HeadInfo>
              <Headline>{t("preview.headline")}</Headline>
            </HeadInfo>
          </StdRow>
        </div>
        <table>
          <tbody>
            <Tr>
              <td>
                <Stat>{t("preview.total")}:</Stat>
              </td>
              <td>
                <BStat>{rawPreviewData.length - 1}</BStat>
              </td>
              <td>
                <Stat>{t("preview.min")}:</Stat>
              </td>
              <td>
                <BStat>{minDate ? formatStdDate(minDate) : "-"}</BStat>
              </td>
            </Tr>
            <Tr>
              <td>
                <Stat>{t("preview.span")}:</Stat>
              </td>
              <td>
                <BStat>
                  {!!minDate && !!maxDate
                    ? formatDateDistance(minDate, maxDate)
                    : "-"}
                </BStat>
              </td>
              <td>
                <Stat>{t("preview.max")}:</Stat>
              </td>
              <td>
                <BStat>{maxDate ? formatStdDate(maxDate) : "-"}</BStat>
              </td>
            </Tr>
          </tbody>
        </table>
      </TopRow>
      {showStats && (
        <div>
          <StatsHeadline>{t("preview.statisticsHeadline")}</StatsHeadline>
          <StatsSubline>{t("preview.statisticsSubline")}</StatsSubline>
          <StatsCard>
            <StatsContainer>
              {rawPreviewData[0].map((col, j) => (
                <ColumnStats
                  key={j}
                  colName={col}
                  columnType={columns[j]}
                  rawColumnData={rawPreviewData.map((row) => row[j])}
                />
              ))}
            </StatsContainer>
          </StatsCard>
        </div>
      )}
    </div>
  );
};

export default PreviewInfo;
