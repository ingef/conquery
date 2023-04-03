import styled from "@emotion/styled";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { ColumnDescription, ResultUrlWithLabel } from "../api/types";
import { StateT } from "../app/reducers";
import PreviewButton from "../button/PreviewButton";
import { QueryResultHistoryButton } from "../button/QueryResultHistoryButton";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import { canUploadResult } from "../user/selectors";

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
  resultUrls: ResultUrlWithLabel[];
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
  const csvUrl = resultUrls.find((ru) => ru.url.endsWith("csv"));
  const canUpload = useSelector<StateT, boolean>(canUploadResult);

  return (
    <Root>
      {isEmpty(resultCount) ? (
        <Text>
          <FaIcon icon={faCheck} left />
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
        <>
          <PreviewButton columns={resultColumns} url={csvUrl.url} />
          {canUpload && (
            <QueryResultHistoryButton
              columns={resultColumns}
              url={csvUrl.url}
              label={resultLabel}
            />
          )}
        </>
      )}
      {resultUrls.length > 0 && (
        <DownloadResultsDropdownButton resultUrls={resultUrls} />
      )}
    </Root>
  );
};

export default QueryResults;
