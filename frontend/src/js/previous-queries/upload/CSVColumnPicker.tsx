import styled from "@emotion/styled";
import {
  faCheckCircle,
  faDownload,
  faExclamationCircle,
  faSpinner,
  faTrash,
  faUpload,
} from "@fortawesome/free-solid-svg-icons";
import { format } from "date-fns";
import { saveAs } from "file-saver";
import type { TFunction } from "i18next";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import { parseCSV, toCSV } from "../../file/csv";
import FaIcon from "../../icon/FaIcon";
import { useActiveLang } from "../../localization/useActiveLang";
import ScrollableList from "../../scrollable-list/ScrollableList";
import WithTooltip from "../../tooltip/WithTooltip";
import { tv } from "../../tv";
import InputSelect from "../../ui-components/InputSelect/InputSelect";

const Row = styled("div")`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 15px;
`;

const td = tv({
  base: "min-w-[150px] overflow-hidden text-ellipsis whitespace-nowrap text-xs",
});
const Th = styled("th")`
  font-size: ${({ theme }) => theme.font.xs};
  vertical-align: top;
  width: 150px;
`;

const Padded = styled("span")`
  padding: 0 6px;
`;
const SxPadded = styled(Padded)`
  display: inline-block;
  margin-top: 10px;
`;

const SxInputSelect = styled(InputSelect)`
  width: 150px;
  text-align: left;
  display: inline-block;
  margin-left: 15px;
`;

const Msg = styled("p")`
  margin: 10px 0 8px;
  &:first-of-type {
    margin-top: 0;
  }
  font-size: ${({ theme }) => theme.font.sm};
  display: flex;
  align-items: center;
`;

const PartialUploadResults = styled("div")`
  box-shadow: 0 0 5px 0 rgb(0, 0, 0, 0.1);
  padding: 15px;
  margin-top: 15px;
`;

const BigIcon = styled(FaIcon)`
  font-size: ${({ theme }) => theme.font.lg};
  margin-right: 7px;
`;
const ErrorIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.red};
`;
const SuccessIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const Buttons = styled("div")`
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  margin-top: 12px;
`;

const SxPrimaryButton = styled(PrimaryButton)`
  margin-left: 10px;
`;
const SxTransparentButton = styled(TransparentButton)`
  margin-left: 10px;
`;

const DownloadUnresolvedButton = styled(TransparentButton)`
  margin-right: auto;
`;

export interface QueryToUploadT {
  format: string[];
  values: string[][];
  label: string;
}

interface PropsT {
  file: File;
  loading: boolean;
  config: QueryUploadConfigT;
  uploadResult: UploadQueryResponseT | null;
  onReset: () => void;
  onCancel: () => void;
  onUpload: (query: QueryToUploadT) => void;
}

type UploadColumnType =
  | string // some ID column format that will be determined by the backend through the "frontend config"
  | "START_DATE" //(a starting day)
  | "END_DATE" // (and end day
  | "DATE_SET" // (a set of date ranges)
  | "EXTRA" // (user supplied additional data per entity)
  | "IGNORE"; // (ignore this column)

const getSelectOptions = (
  config: PropsT["config"],
  locale: ReturnType<typeof useActiveLang>,
  t: TFunction,
): { label: string; value: string }[] => [
  { label: t("csvColumnPicker.ignore"), value: "IGNORE" },
  ...config.ids.map(({ name, label }) => ({
    label: label[locale] || t("common.missingLabel"),
    value: name,
  })),
  { label: t("csvColumnPicker.dateSet"), value: "DATE_SET" },
  { label: t("csvColumnPicker.startDate"), value: "START_DATE" },
  { label: t("csvColumnPicker.endDate"), value: "END_DATE" },
  { label: t("csvColumnPicker.extra"), value: "EXTRA" },
];

// Parses the file whenever it or the delimiter changes and resets the column mapping to IGNORE
const useParsedCSV = (
  file: File,
  delimiter: string,
  setCSVHeader: (header: UploadColumnType[]) => void,
) => {
  const [csv, setCSV] = useState<string[][]>([]);
  const [csvLoading, setCSVLoading] = useState(false);

  useEffect(() => {
    async function parse() {
      try {
        setCSVLoading(true);
        const result = await parseCSV(file, delimiter);
        setCSVLoading(false);

        if (result.data.length === 0) return;

        setCSV(result.data);
        setCSVHeader(new Array(result.data[0].length).fill("IGNORE"));
      } catch {
        setCSVLoading(false);
      }
    }

    parse();
  }, [file, delimiter, setCSVHeader]);

  return { csv, csvLoading };
};

const CSVPreviewTable = ({
  csv,
  csvLoading,
  csvHeader,
  selectOptions,
  onHeaderChange,
}: {
  csv: string[][];
  csvLoading: boolean;
  csvHeader: UploadColumnType[];
  selectOptions: { label: string; value: string }[];
  onHeaderChange: (header: UploadColumnType[]) => void;
}) => {
  const { t } = useTranslation();
  return (
    <table className="table-fixed [&_td]:px-1 [&_th]:px-1">
      <thead>
        {csvLoading && (
          <tr>
            <th>{t("csvColumnPicker.loading")}</th>
          </tr>
        )}
        {csv.length > 0 &&
          csv.slice(0, 1).map((row, j) => (
            <tr key={j}>
              {row.map((cell, i) => (
                <Th key={cell + i}>
                  <InputSelect
                    smallMenu
                    options={selectOptions}
                    value={
                      selectOptions.find((o) => o.value === csvHeader[i]) ||
                      selectOptions[0]
                    }
                    onChange={(value) => {
                      if (value) {
                        onHeaderChange([
                          ...csvHeader.slice(0, i),
                          value.value as UploadColumnType,
                          ...csvHeader.slice(i + 1),
                        ]);
                      }
                    }}
                  />
                  <SxPadded>{cell}</SxPadded>
                </Th>
              ))}
            </tr>
          ))}
      </thead>
      <tbody>
        {csv.length > 0 &&
          csv.slice(1, 6).map((row, j) => (
            <tr key={j}>
              {row.map((cell, i) => (
                <td key={cell + i} className={td()}>
                  <Padded>{cell}</Padded>
                </td>
              ))}
            </tr>
          ))}
        {csv.length > 6 && (
          <tr>
            {new Array(csv[0].length).fill(null).map((_, j) => (
              <td key={j} className={td()}>
                <Padded>...</Padded>
              </td>
            ))}
          </tr>
        )}
      </tbody>
    </table>
  );
};

const CSVColumnPicker = ({
  file,
  loading,
  config,
  uploadResult,
  onUpload,
  onReset,
  onCancel,
}: PropsT) => {
  const { t } = useTranslation();
  const locale = useActiveLang();
  const [delimiter, setDelimiter] = useState<string>(";");
  const [csvHeader, setCSVHeader] = useState<UploadColumnType[]>([]);
  const { csv, csvLoading } = useParsedCSV(file, delimiter, setCSVHeader);

  const SELECT_OPTIONS = getSelectOptions(config, locale, t);

  const DELIMITER_OPTIONS = [
    { label: `${t("csvColumnPicker.semicolon")} ( ; )`, value: ";" },
    { label: `${t("csvColumnPicker.comma")} ( , )`, value: "," },
    { label: `${t("csvColumnPicker.colon")} ( : )`, value: ":" },
  ];

  function uploadQuery() {
    onUpload({
      format: csvHeader,
      values: csv,
      label: file.name,
    });
  }

  function downloadUnresolved() {
    if (!uploadResult) return;

    const unresolved = toCSV(
      [...uploadResult.unresolvedId, ...uploadResult.unreadableDate],
      delimiter,
    );

    const blob = new Blob([unresolved], { type: "text/csv;charset=utf-8" });
    const today = format(new Date(), "yyyy-MM-dd-HH-mm-ss");
    const filename = `unresolved-${today}.csv`;

    saveAs(blob, filename);
  }

  const ignoringAllColumns = csvHeader.every((h) => h === "IGNORE");
  const hasAtLeastOneIdColumn = config.ids
    .map(({ name }) => name)
    .some((id) => csvHeader.includes(id));

  const uploadDisabled =
    !hasAtLeastOneIdColumn || ignoringAllColumns || loading || csv.length === 0;

  return (
    <div>
      <Row>
        <div className="flex items-center gap-3">
          <div className="flex flex-col text-sm">
            <code className="font-bold">{file.name}</code>
            <code>{csv.length} Zeilen</code>
          </div>
          <WithTooltip text={t("common.clear")}>
            <IconButton frame icon={faTrash} onClick={onReset} />
          </WithTooltip>
        </div>
        {csv.length > 0 && (
          <SxInputSelect
            label={t("csvColumnPicker.delimiter")}
            onChange={(val) => {
              if (val) setDelimiter(val.value as string);
            }}
            value={
              DELIMITER_OPTIONS.find((option) => option.value === delimiter) ||
              null
            }
            options={DELIMITER_OPTIONS}
          />
        )}
      </Row>
      <div className="overflow-hidden rounded-sm py-3 px-2 border w-full">
        <div className="overflow-x-auto">
          <CSVPreviewTable
            csv={csv}
            csvLoading={csvLoading}
            csvHeader={csvHeader}
            selectOptions={SELECT_OPTIONS}
            onHeaderChange={setCSVHeader}
          />
        </div>
      </div>
      {uploadResult && (
        <PartialUploadResults>
          <Msg>
            {uploadResult.resolved > 0 && <SuccessIcon icon={faCheckCircle} />}
            {t("csvColumnPicker.resolved", { count: uploadResult.resolved })}
          </Msg>
          {uploadResult.unreadableDate.length > 0 && (
            <>
              <Msg>
                <ErrorIcon icon={faExclamationCircle} />
                {t("csvColumnPicker.unreadableDate", {
                  count: uploadResult.unreadableDate.length,
                })}
              </Msg>
              <ScrollableList
                maxVisibleItems={3}
                fullWidth
                items={
                  uploadResult.unreadableDate.map((row) =>
                    row.join(delimiter),
                  ) || []
                }
              />
            </>
          )}
          {uploadResult.unresolvedId.length > 0 && (
            <>
              <Msg>
                <ErrorIcon icon={faExclamationCircle} />
                {t("csvColumnPicker.unresolvedId", {
                  count: uploadResult.unresolvedId.length,
                })}
              </Msg>
              <ScrollableList
                maxVisibleItems={3}
                fullWidth
                items={
                  uploadResult.unresolvedId.map((row) => row.join(delimiter)) ||
                  []
                }
              />
            </>
          )}
        </PartialUploadResults>
      )}
      <Buttons>
        {uploadResult &&
          (uploadResult.unreadableDate.length > 0 ||
            uploadResult.unresolvedId.length > 0) && (
            <DownloadUnresolvedButton onClick={downloadUnresolved}>
              <FaIcon icon={faDownload} />{" "}
              {t("uploadQueryResultsModal.downloadUnresolved", {
                count:
                  uploadResult.unreadableDate.length +
                  uploadResult.unresolvedId.length,
              })}
            </DownloadUnresolvedButton>
          )}
        {uploadResult && (
          <SxPrimaryButton disabled={uploadDisabled} onClick={uploadQuery}>
            {loading ? (
              <FaIcon white icon={faSpinner} />
            ) : (
              <FaIcon white left icon={faUpload} />
            )}{" "}
            {t("uploadQueryResultsModal.uploadAgain")}
          </SxPrimaryButton>
        )}
        {uploadResult ? (
          <SxTransparentButton disabled={loading} onClick={onCancel}>
            {t("common.done")}
          </SxTransparentButton>
        ) : (
          <SxPrimaryButton disabled={uploadDisabled} onClick={uploadQuery}>
            {loading ? (
              <FaIcon white icon={faSpinner} />
            ) : (
              <FaIcon left white icon={faUpload} />
            )}{" "}
            {t("uploadQueryResultsModal.upload")}
          </SxPrimaryButton>
        )}
      </Buttons>
    </div>
  );
};

export default CSVColumnPicker;
