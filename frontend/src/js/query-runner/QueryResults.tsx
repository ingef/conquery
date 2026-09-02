import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { ColumnDescription, ResultUrlWithLabel } from "../api/types";
import type { StateT } from "../app/reducers";
import PreviewButton from "../button/PreviewButton";
import { QueryResultHistoryButton } from "../button/QueryResultHistoryButton";
import { isEmpty } from "../common/helpers/commonHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import { canViewEntityPreview, canViewQueryPreview } from "../user/selectors";
import DownloadResultsDropdownButton from "./DownloadResultsDropdownButton";

const root = tv({
  base: ["flex items-center justify-end", "gap-[7px]"],
});

const text = tv({
  base: ["m-0", "leading-none", "text-sm"],
});

const lgText = tv({
  base: ["m-0", "leading-none", "text-xl", "whitespace-nowrap"],
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
        <p className={text()}>
          <FaIcon icon={faCheck} left />
          {t("queryRunner.endSuccess")}
        </p>
      ) : (
        <p className={lgText()}>
          <span className="font-bold">{resultCount}</span>{" "}
          {queryType === "SECONDARY_ID_QUERY"
            ? t("queryRunner.resultCountSecondaryIdQuery")
            : t("queryRunner.resultCount")}
        </p>
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
