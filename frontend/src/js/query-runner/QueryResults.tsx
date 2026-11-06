import { CheckIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { ColumnDescription, ResultUrlWithLabel } from "../api/types";
import type { StateT } from "../app/reducers";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import { QueryResultHistoryButton } from "../entity-history/QueryResultHistoryButton";
import PreviewButton from "../preview/PreviewButton";
import { C1, C2 } from "../ui-components/Typography";
import { canViewEntityPreview, canViewQueryPreview } from "../user/selectors";
import DownloadResultsDropdownButton from "./DownloadResultsDropdownButton";

const root = tv({
  base: ["flex items-center justify-end", "gap-[7px]"],
});

const QueryResults = ({
  resultLabel,
  resultUrls,
  resultCount,
  resultColumns,
  queryType,
  previewAvailable,
}: {
  resultLabel: string;
  resultUrls: ResultUrlWithLabel[];
  resultCount?: number | null; // For forms, won't usually have a count
  resultColumns?: ColumnDescription[] | null; // For forms, won't usually have resultColumns
  queryType?: "CONCEPT_QUERY" | "SECONDARY_ID_QUERY";
  previewAvailable?: boolean; // Backend decides, e.g. most forms have no preview
}) => {
  const { t } = useTranslation();
  const csvUrl = resultUrls.find(({ url }) => url.endsWith("csv"));
  const canViewHistory = useSelector<StateT, boolean>(canViewEntityPreview);
  const canViewPreview = useSelector<StateT, boolean>(canViewQueryPreview);

  return (
    <div className={root()}>
      {isEmpty(resultCount) ? (
        <div className="flex items-center gap-[10px]">
          <CheckIcon />
          <C2 as="span">{t("queryRunner.endSuccess")}</C2>
        </div>
      ) : (
        <div className="whitespace-nowrap">
          <C1>
            <C1 as="span" strong>
              {resultCount}
            </C1>{" "}
            {queryType === "SECONDARY_ID_QUERY"
              ? t("queryRunner.resultCountSecondaryIdQuery")
              : t("queryRunner.resultCount")}
          </C1>
        </div>
      )}
      {canViewPreview && previewAvailable && <PreviewButton />}
      {!!csvUrl && canViewHistory && exists(resultColumns) && (
        <QueryResultHistoryButton
          columns={resultColumns}
          url={csvUrl.url}
          label={resultLabel}
        />
      )}
      {resultUrls.length > 0 && (
        <DownloadResultsDropdownButton resultUrls={resultUrls} />
      )}
    </div>
  );
};

export default QueryResults;
