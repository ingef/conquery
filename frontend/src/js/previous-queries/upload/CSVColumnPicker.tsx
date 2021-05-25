import styled from "@emotion/styled";
import React, { useState, useEffect, FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import PrimaryButton from "../../button/PrimaryButton";
import { parseCSV } from "../../file/csv";
import InputSelect from "../../form-components/InputSelect";
import ReactSelect from "../../form-components/ReactSelect";
import FaIcon from "../../icon/FaIcon";

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
`;

const Td = styled("td")`
  font-size: ${({ theme }) => theme.font.xs};
`;
const Th = styled("th")`
  font-size: ${({ theme }) => theme.font.xs};
  min-width: 125px;
`;

const SxIconButton = styled(IconButton)`
  margin-left: 10px;
`;

const FileName = styled("code")`
  margin: 0;
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

export interface ExternalQueryT {
  format: string[];
  values: string[][];
}

interface PropsT {
  file: File;
  loading: boolean;
  onReset: () => void;
  onUpload: (query: ExternalQueryT) => void;
}

type UploadColumnType =
  | "ID" // (some string)
  | "EVENT_DATE" // (a single day)
  | "START_DATE" //(a starting day)
  | "END_DATE" // (and end day
  | "DATE_RANGE" // (two days)
  | "DATE_SET" // (a set of date ranges)
  | "IGNORE"; // (ignore this column)

const CSVColumnPicker: FC<PropsT> = ({ file, loading, onUpload, onReset }) => {
  const { t } = useTranslation();
  const [csv, setCSV] = useState<string[][]>([]);
  const [delimiter, setDelimiter] = useState<string>(";");
  const [csvHeader, setCSVHeader] = useState<UploadColumnType[]>([]);
  const [csvLoading, setCSVLoading] = useState(false);

  const SELECT_OPTIONS: { label: string; value: UploadColumnType }[] = [
    { label: t("csvColumnPicker.id"), value: "ID" },
    { label: t("csvColumnPicker.dateSet"), value: "DATE_SET" },
    { label: t("csvColumnPicker.startDate"), value: "START_DATE" },
    { label: t("csvColumnPicker.endDate"), value: "END_DATE" },
    { label: t("csvColumnPicker.ignore"), value: "IGNORE" },
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

          // External queries (uploaded lists) usually contain three or four columns.
          // The first two columns are IDs, which will be concatenated
          // The other two columns are date ranges
          if (firstRow.length >= 4) {
            initialCSVHeader[0] = "ID";
            initialCSVHeader[1] = "ID";
            initialCSVHeader[2] = "START_DATE";
            initialCSVHeader[3] = "END_DATE";
          } else if (firstRow.length === 3) {
            initialCSVHeader = ["ID", "ID", "DATE_SET"];
          } else if (firstRow.length === 2) {
            initialCSVHeader = ["ID", "DATE_SET"];
          } else {
            initialCSVHeader = ["ID"];
          }

          setCSVHeader(initialCSVHeader);
        }
      } catch (e) {
        setCSVLoading(false);
      }
    }

    if (file) {
      parse();
    }
  }, [file, delimiter]);

  function uploadQuery() {
    onUpload({
      format: csvHeader,
      values: csv,
    });
  }

  return (
    <div>
      <Row>
        <Grow>
          <FileName>{file.name}</FileName>
          <SxIconButton frame regular icon="trash-alt" onClick={onReset} />
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
      <PrimaryButton
        disabled={loading || csv.length === 0}
        onClick={uploadQuery}
      >
        {loading && <FaIcon white icon="spinner" />}{" "}
        {t("uploadQueryResultsModal.upload")}
      </PrimaryButton>
    </div>
  );
};

export default CSVColumnPicker;
