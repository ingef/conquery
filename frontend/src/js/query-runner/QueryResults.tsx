import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { ColumnDescription } from "../api/types";
import DownloadButton from "../button/DownloadButton";
import HistoryButton from "../button/HistoryButton";
import PreviewButton from "../button/PreviewButton";
import { useIsHistoryEnabled } from "../common/feature-flags/useIsHistoryEnabled";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";

import DownloadResultsDropdownButton from "./DownloadResultsDropdownButton";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 7px;
`;

const Text = styled("p")`
  margin: 0;
  line-height: 1;
  font-size: ${({ theme }) => theme.font.sm};
`;

const LgText = styled(Text)`
  font-size: ${({ theme }) => theme.font.lg};
  white-space: nowrap;
`;

const Bold = styled("span")`
  font-weight: 700;
`;

interface PropsT {
  resultLabel: string;
  resultUrls: string[];
  resultCount?: number | null; // For forms, won't usually have a count
  resultColumns?: ColumnDescription[] | null; // For forms, won't usually have resultColumns
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
}

const QueryResults: FC<PropsT> = ({
  resultLabel,
  resultUrls,
  resultCount,
  resultColumns,
  queryType,
}) => {
  const { t } = useTranslation();
  const isHistoryEnabled = useIsHistoryEnabled();
  const csvUrl = resultUrls.find((url) => url.endsWith("csv"));

  return (
    <Root>
      {isEmpty(resultCount) ? (
        <Text>
          <FaIcon icon="check" left />
          {t("queryRunner.endSuccess")}
        </Text>
      ) : (
        <LgText>
          <Bold>{resultCount}</Bold>{" "}
          {queryType === "SECONDARY_ID_QUERY"
            ? t("queryRunner.resultCountSecondaryIdQuery")
            : t("queryRunner.resultCount")}
        </LgText>
      )}
      {!!csvUrl && exists(resultColumns) && (
        <PreviewButton columns={resultColumns} url={csvUrl} />
      )}
      {isHistoryEnabled && !!csvUrl && exists(resultColumns) && (
        <HistoryButton
          columns={resultColumns}
          url={csvUrl}
          label={resultLabel}
        />
      )}
      {resultUrls.length > 0 && (
        <DownloadResultsDropdownButton resultUrls={resultUrls} />
      )}
    </Root>
  );
};

export default QueryResults;
