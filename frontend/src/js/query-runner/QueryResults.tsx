import styled from "@emotion/styled";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";

import type { ColumnDescription } from "../api/types";
import DownloadButton from "../button/DownloadButton";
import PreviewButton from "../button/PreviewButton";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";

const Root = styled("div")`
  display: flex;
  align-items: center;
  justify-content: flex-end;
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

const SxDownloadButton = styled(DownloadButton)`
  margin-left: 10px;
`;

const SxPreviewButton = styled(PreviewButton)`
  margin-left: 10px;
`;

const Bold = styled("span")`
  font-weight: 700;
`;

interface PropsT {
  resultUrls: string[];
  resultCount?: number | null; // For forms, won't usually have a count
  resultColumns?: ColumnDescription[] | null; // For forms, won't usually have resultColumns
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
}

const QueryResults: FC<PropsT> = ({
  resultUrls,
  resultCount,
  resultColumns,
  queryType,
}) => {
  const { t } = useTranslation();
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
        <SxPreviewButton columns={resultColumns} url={csvUrl} />
      )}
      {resultUrls.map((url) => {
        const ending = url.split(".").reverse()[0];

        return (
          <SxDownloadButton key={url} frame url={url}>
            {ending.toUpperCase()}
          </SxDownloadButton>
        );
      })}
    </Root>
  );
};

export default QueryResults;
