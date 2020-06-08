import React, { FC } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import TransparentButton from "../button/TransparentButton";
import {
  formatStdDate,
  formatDateDistance,
} from "../common/helpers/dateHelper";
import type { ColumnDescriptionType } from "./Preview";
import { faColumns } from "@fortawesome/free-solid-svg-icons";
import ColumnStats from "./ColumnStats";

const TopRow = styled("div")`
  margin: 12px 0 20px;
  width: 100%;
  display: flex;
  align-items: center;
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

const Explanation = styled("p")`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 0;
`;

const HeadInfo = styled("div")`
  margin: 0 20px;
`;

const Tr = styled("tr")`
  line-height: 1;
`;

interface PropsT {
  columns: ColumnDescriptionType[];
  rawPreviewData: string[][];
  onClose: () => void;
  minDate: Date | null;
  maxDate: Date | null;
  rowsLimit: number;
}

const PreviewInfo: FC<PropsT> = ({
  rawPreviewData,
  columns,
  onClose,
  minDate,
  maxDate,
  rowsLimit,
}) => {
  if (rawPreviewData.length < 2) return null;

  return (
    <TopRow>
      <div>
        <StdRow>
          <TransparentButton icon="times" onClick={onClose}>
            {T.translate("common.back")}
          </TransparentButton>
          <HeadInfo>
            <Headline>{T.translate("preview.headline")}</Headline>
            <Explanation>
              {T.translate("preview.explanation", { count: rowsLimit })}
            </Explanation>
          </HeadInfo>
        </StdRow>
        <table>
          <tbody>
            {rawPreviewData[0].map((col, j) => (
              <>
                <Tr>
                  <td>
                    <BStat>{col}</BStat>
                  </td>
                  <td>{columns[j]}</td>
                </Tr>
                <Tr>
                  <td colSpan={2}>
                    <ColumnStats
                      columnType={columns[j]}
                      rawColumnData={rawPreviewData.map((row) => row[j])}
                    />
                  </td>
                </Tr>
              </>
            ))}
          </tbody>
        </table>
      </div>
      <table>
        <tbody>
          <Tr>
            <td>
              <Stat>{T.translate("preview.total")}:</Stat>
            </td>
            <td>
              <BStat>{rawPreviewData.length - 1}</BStat>
            </td>
          </Tr>
          <Tr>
            <td>
              <Stat>{T.translate("preview.min")}:</Stat>
            </td>
            <td>
              <BStat>{minDate ? formatStdDate(minDate) : "-"}</BStat>
            </td>
          </Tr>
          <Tr>
            <td>
              <Stat>{T.translate("preview.max")}:</Stat>
            </td>
            <td>
              <BStat>{maxDate ? formatStdDate(maxDate) : "-"}</BStat>
            </td>
          </Tr>
          <Tr>
            <td>
              <Stat>{T.translate("preview.span")}:</Stat>
            </td>
            <td>
              <BStat>
                {!!minDate && !!maxDate
                  ? formatDateDistance(minDate, maxDate)
                  : "-"}
              </BStat>
            </td>
          </Tr>
        </tbody>
      </table>
    </TopRow>
  );
};

export default PreviewInfo;
