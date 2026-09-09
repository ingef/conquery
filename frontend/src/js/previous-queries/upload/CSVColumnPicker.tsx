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
import { tv } from "tailwind-variants";
import type { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";
import { TransparentButton } from "../../button/TransparentButton";
import { parseCSV, toCSV } from "../../file/csv";
import FaIcon from "../../icon/FaIcon";
import { useActiveLang } from "../../localization/useActiveLang";
import ScrollableList from "../../scrollable-list/ScrollableList";
import InputSelect from "../../ui-components/InputSelect/InputSelect";
import WithTooltip from "../../ui-components/WithTooltip";

const td = tv({
  base: "min-w-[150px] overflow-hidden text-ellipsis whitespace-nowrap text-xs",
});

const th = tv({
  base: ["w-[150px]", "align-top", "text-xs"],
});

const msg = tv({
  base: ["flex items-center", "mt-[10px] mb-2 first-of-type:mt-0", "text-sm"],
});

const partialUploadResults = tv({
  base: ["mt-[15px]", "p-[15px]", "shadow-[0_0_5px_0_rgba(0,0,0,0.1)]"],
});

const bigIcon = tv({
  base: ["mr-[7px]", "text-xl"],
  variants: {
    kind: {
      error: "text-red",
      success: "text-green",
    },
  },
});

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
                <th key={cell + i} className={th()}>
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
                  <span className="mt-[10px] inline-block px-[6px]">
                    {cell}
                  </span>
                </th>
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
                  <span className="px-[6px]">{cell}</span>
                </td>
              ))}
            </tr>
          ))}
        {csv.length > 6 && (
          <tr>
            {new Array(csv[0].length).fill(null).map((_, j) => (
              <td key={j} className={td()}>
                <span className="px-[6px]">...</span>
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
      <div className="mb-[15px] flex items-end justify-between">
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
          <InputSelect
            className="ml-[15px] inline-block w-[150px] text-left"
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
      </div>
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
        <div className={partialUploadResults()}>
          <p className={msg()}>
            {uploadResult.resolved > 0 && (
              <FaIcon
                className={bigIcon({ kind: "success" })}
                icon={faCheckCircle}
              />
            )}
            {t("csvColumnPicker.resolved", { count: uploadResult.resolved })}
          </p>
          {uploadResult.unreadableDate.length > 0 && (
            <>
              <p className={msg()}>
                <FaIcon
                  className={bigIcon({ kind: "error" })}
                  icon={faExclamationCircle}
                />
                {t("csvColumnPicker.unreadableDate", {
                  count: uploadResult.unreadableDate.length,
                })}
              </p>
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
              <p className={msg()}>
                <FaIcon
                  className={bigIcon({ kind: "error" })}
                  icon={faExclamationCircle}
                />
                {t("csvColumnPicker.unresolvedId", {
                  count: uploadResult.unresolvedId.length,
                })}
              </p>
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
        </div>
      )}
      <div className="mt-3 flex items-end justify-end">
        {uploadResult &&
          (uploadResult.unreadableDate.length > 0 ||
            uploadResult.unresolvedId.length > 0) && (
            <TransparentButton className="mr-auto" onClick={downloadUnresolved}>
              <FaIcon icon={faDownload} />{" "}
              {t("uploadQueryResultsModal.downloadUnresolved", {
                count:
                  uploadResult.unreadableDate.length +
                  uploadResult.unresolvedId.length,
              })}
            </TransparentButton>
          )}
        {uploadResult && (
          <PrimaryButton
            className="ml-[10px]"
            disabled={uploadDisabled}
            onClick={uploadQuery}
          >
            {loading ? (
              <FaIcon white icon={faSpinner} />
            ) : (
              <FaIcon white left icon={faUpload} />
            )}{" "}
            {t("uploadQueryResultsModal.uploadAgain")}
          </PrimaryButton>
        )}
        {uploadResult ? (
          <TransparentButton
            className="ml-[10px]"
            disabled={loading}
            onClick={onCancel}
          >
            {t("common.done")}
          </TransparentButton>
        ) : (
          <PrimaryButton
            className="ml-[10px]"
            disabled={uploadDisabled}
            onClick={uploadQuery}
          >
            {loading ? (
              <FaIcon white icon={faSpinner} />
            ) : (
              <FaIcon left white icon={faUpload} />
            )}{" "}
            {t("uploadQueryResultsModal.upload")}
          </PrimaryButton>
        )}
      </div>
    </div>
  );
};

export default CSVColumnPicker;
