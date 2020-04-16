import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { parseCSV } from "../../file/csv";

import PrimaryButton from "../../button/PrimaryButton";
import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";

import ReactSelect from "../../form-components/ReactSelect";
import InputSelect from "../../form-components/InputSelect";

type ExternalQueryT = {
  format: string[];
  values: string[][];
};

type PropsT = {
  file: File;
  loading: boolean;
  onReset: () => void;
  onUpload: (query: ExternalQueryT) => void;
};

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

export default ({ file, loading, onUpload, onReset }: PropsT) => {
  const [csv, setCSV] = React.useState([]);
  const [delimiter, setDelimiter] = React.useState(";");
  const [csvHeader, setCSVHeader] = React.useState([]);
  const [csvLoading, setCSVLoading] = React.useState(false);

  // Theoretically possible in the backend:
  // ID (some string)
  // EVENT_DATE (a single day),
  // START_DATE (a starting day),
  // END_DATE (and end day,
  // DATE_RANGE (two days),
  // DATE_SET (a set of date ranges),
  // IGNORE (ignore this column);
  const SELECT_OPTIONS = [
    { label: T.translate("csvColumnPicker.id"), value: "ID" },
    { label: T.translate("csvColumnPicker.dateSet"), value: "DATE_SET" },
    { label: T.translate("csvColumnPicker.startDate"), value: "START_DATE" },
    { label: T.translate("csvColumnPicker.endDate"), value: "END_DATE" },
    { label: T.translate("csvColumnPicker.ignore"), value: "IGNORE" }
  ];

  const DELIMITER_OPTIONS = [
    { label: T.translate("csvColumnPicker.semicolon") + " ( ; )", value: ";" },
    { label: T.translate("csvColumnPicker.comma") + " ( , )", value: "," },
    { label: T.translate("csvColumnPicker.colon") + " ( : )", value: ":" }
  ];

  React.useEffect(() => {
    async function parse(f, d) {
      try {
        setCSVLoading(true);

        const parsed = await parseCSV(f, d);
        const { result } = parsed;

        setCSVLoading(false);

        if (result.data.length > 0) {
          setCSV(result.data);
          setCSVHeader(
            // This is experimental still.
            // External queries (uploaded lists) usually contain three or four columns.
            // The first two columns are IDs, which will be concatenated
            // The other two columns are date ranges
            // We simply assume that the data is in this format by default
            result.data[0].length >= 4
              ? ["ID", "ID", "START_DATE", "END_DATE"]
              : ["ID", "ID", "DATE_SET"]
          );
        }
      } catch (e) {
        setCSVLoading(false);
      }
    }

    if (!!file) {
      parse(file, delimiter);
    }
  }, [file, delimiter]);

  function uploadQuery() {
    onUpload({
      format: csvHeader,
      values: csv
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
            label={T.translate("csvColumnPicker.delimiter")}
            input={{
              onChange: setDelimiter,
              value: delimiter,
              defaultValue: DELIMITER_OPTIONS[0]
            }}
            options={DELIMITER_OPTIONS}
          />
        )}
      </Row>
      <Table>
        <thead>
          {csvLoading && (
            <tr>
              <th>{T.translate("csvColumnPicker.loading")}</th>
            </tr>
          )}
          {csv.length > 0 &&
            csv.slice(0, 1).map((row, j) => (
              <tr key={j}>
                {row.map((cell, i) => (
                  <Th key={cell + i}>
                    <ReactSelect
                      small
                      options={SELECT_OPTIONS}
                      value={
                        SELECT_OPTIONS.find(o => o.value === csvHeader[i]) ||
                        SELECT_OPTIONS[0]
                      }
                      onChange={value =>
                        setCSVHeader([
                          ...csvHeader.slice(0, i),
                          value.value,
                          ...csvHeader.slice(i + 1)
                        ])
                      }
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
        {T.translate("uploadQueryResultsModal.upload")}
      </PrimaryButton>
    </div>
  );
};
