import styled from "@emotion/styled";
import format from "date-fns/format";
import { saveAs } from "file-saver";
import React, { useState, useEffect, FC } from "react";
import { useTranslation } from "react-i18next";

import { QueryUploadConfigT, UploadQueryResponseT } from "../../api/types";
import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";
import TransparentButton from "../../button/TransparentButton";
import { parseCSV, toCSV } from "../../file/csv";
import InputSelect from "../../form-components/InputSelect";
import ReactSelect from "../../form-components/ReactSelect";
import FaIcon from "../../icon/FaIcon";
import { useActiveLang } from "../../localization/useActiveLang";
import ScrollableList from "../../scrollable-list/ScrollableList";
import WithTooltip from "../../tooltip/WithTooltip";

const Row = styled("div")`
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 15px;
`;

const Grow = styled("div")`
  display: flex;
  align-items: center;
`;

const Table = styled("table")`
  margin: 10px 0;
  border: 1px solid #ccc;
  text-align: left;
  width: 100%;
  padding: 10px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.1);
  table-layout: fixed;
  max-width: 1000px;
`;

const Td = styled("td")`
  font-size: ${({ theme }) => theme.font.xs};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;
const Th = styled("th")`
  font-size: ${({ theme }) => theme.font.xs};
  vertical-align: top;
`;

const FileName = styled("code")`
  display: block;
  margin: 0 15px 0 0;
`;

const BoldFileName = styled(FileName)`
  margin-bottom: 3px;
  font-weight: 700;
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
  | "EVENT_DATE" // (a single day)
  | "START_DATE" //(a starting day)
  | "END_DATE" // (and end day
  | "DATE_RANGE" // (two days)
  | "DATE_SET" // (a set of date ranges)
  | "IGNORE"; // (ignore this column)

const CSVColumnPicker: FC<PropsT> = ({
  file,
  loading,
  config,
  uploadResult,
  onUpload,
  onReset,
  onCancel,
}) => {
  const { t } = useTranslation();
  const locale = useActiveLang();
  const [csv, setCSV] = useState<string[][]>([]);
  const [delimiter, setDelimiter] = useState<string>(";");
  const [csvHeader, setCSVHeader] = useState<UploadColumnType[]>([]);
  const [csvLoading, setCSVLoading] = useState(false);

  const SELECT_OPTIONS: { label: string; value: string }[] = [
    { label: t("csvColumnPicker.ignore"), value: "IGNORE" },
    ...config.ids.map(({ name, label }) => {
      const labelWithFallback: string =
        label[locale] || t("common.missingLabel");

      return {
        label: labelWithFallback,
        value: name,
      };
    }),
    { label: t("csvColumnPicker.dateRange"), value: "DATE_RANGE" },
    { label: t("csvColumnPicker.dateSet"), value: "DATE_SET" },
    { label: t("csvColumnPicker.startDate"), value: "START_DATE" },
    { label: t("csvColumnPicker.endDate"), value: "END_DATE" },
    { label: t("csvColumnPicker.eventDate"), value: "EVENT_DATE" },
  ];

  const DELIMITER_OPTIONS = [
    { label: t("csvColumnPicker.semicolon") + " ( ; )", value: ";" },
    { label: t("csvColumnPicker.comma") + " ( , )", value: "," },
    { label: t("csvColumnPicker.colon") + " ( : )", value: ":" },
  ];

  useEffect(() => {
    async function parse() {
      try {
        setCSVLoading(true);

        const parsed = await parseCSV(file, delimiter);
        const { result } = parsed;

        setCSVLoading(false);

        if (result.data.length > 0) {
          setCSV(result.data);

          const firstRow = result.data[0];

          let initialCSVHeader = new Array(firstRow.length).fill("IGNORE");

          // NOTE: IT WAS A PREVIOUS REQUIREMENT TO INITIALIZE THE HEADER WITH CERTAIN
          //       DEFAULT VALUES DEPENDING ON THE NUMBER OF COLUMNS IN THE CSV.
          //       SINCE WE'LL WANT TO IMPROVE THE INITIALIZATION MECHANISM SOON,
          //       I'M LEAVING THE CODE HERE FOR THE MOMENT:
          // External queries (uploaded lists) usually contain three or four columns.
          // The first two columns are IDs, which will be concatenated
          // The other two columns are date ranges
          // const initialIdName =
          //   config.ids.length > 0 ? config.ids[0].name : "IGNORE";
          // if (firstRow.length >= 4) {
          //   initialCSVHeader[0] = initialIdName;
          //   initialCSVHeader[1] = initialIdName;
          //   initialCSVHeader[2] = "START_DATE";
          //   initialCSVHeader[3] = "END_DATE";
          // } else if (firstRow.length === 3) {
          //   initialCSVHeader = [initialIdName, initialIdName, "DATE_SET"];
          // } else if (firstRow.length === 2) {
          //   initialCSVHeader = [initialIdName, "DATE_SET"];
          // } else {
          //   initialCSVHeader = [initialIdName];
          // }

          setCSVHeader(initialCSVHeader);
        }
      } catch (e) {
        setCSVLoading(false);
      }
    }

    parse();
  }, [file, delimiter, config.ids]);

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
        <Grow>
          <div>
            <BoldFileName>{file.name}</BoldFileName>
            <FileName>{csv.length} Zeilen</FileName>
          </div>
          <WithTooltip text={t("common.clear")}>
            <IconButton frame regular icon="trash-alt" onClick={onReset} />
          </WithTooltip>
        </Grow>
        {csv.length > 0 && (
          <SxInputSelect
            label={t("csvColumnPicker.delimiter")}
            input={{
              onChange: (val) => {
                if (val) setDelimiter(val);
              },
              value: delimiter,
            }}
            options={DELIMITER_OPTIONS}
          />
        )}
      </Row>
      <Table>
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
                    <ReactSelect<false>
                      small
                      maxMenuHeight={200}
                      options={SELECT_OPTIONS}
                      value={
                        SELECT_OPTIONS.find((o) => o.value === csvHeader[i]) ||
                        SELECT_OPTIONS[0]
                      }
                      onChange={(value) => {
                        if (value) {
                          setCSVHeader([
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
                  <Td key={cell + i}>
                    <Padded>{cell}</Padded>
                  </Td>
                ))}
              </tr>
            ))}
          {csv.length > 6 && (
            <tr>
              {new Array(csv[0].length).fill(null).map((_, j) => (
                <Td key={j}>
                  <Padded>...</Padded>
                </Td>
              ))}
            </tr>
          )}
        </tbody>
      </Table>
      {uploadResult && (
        <PartialUploadResults>
          <Msg>
            {uploadResult.resolved > 0 && <SuccessIcon icon="check-circle" />}
            {t("csvColumnPicker.resolved", { count: uploadResult.resolved })}
          </Msg>
          {uploadResult.unreadableDate.length > 0 && (
            <>
              <Msg>
                <ErrorIcon icon="exclamation-circle" />
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
                <ErrorIcon icon="exclamation-circle" />
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
              <FaIcon icon="download" />{" "}
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
              <FaIcon white icon="spinner" />
            ) : (
              <FaIcon white left icon="upload" />
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
              <FaIcon white icon="spinner" />
            ) : (
              <FaIcon left white icon="upload" />
            )}{" "}
            {t("uploadQueryResultsModal.upload")}
          </SxPrimaryButton>
        )}
      </Buttons>
    </div>
  );
};

export default CSVColumnPicker;
