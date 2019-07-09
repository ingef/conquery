// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import parseCSV from "../../file/parseCSV";

import PrimaryButton from "../../button/PrimaryButton";
import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";

import ReactSelect from "../../form-components/ReactSelect";

type ExternalQueryT = {
  format: string[],
  values: string[][]
};

type PropsT = {
  file: File,
  loading: boolean,
  onReset: () => void,
  onUpload: (query: ExternalQueryT) => void
};

const Row = styled("div")`
  display: flex;
  align-items: center;
  margin-bottom: 15px;
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

// Theoretically possible in the backend:
// ID (some string)
// EVENT_DATE (a single day),
// START_DATE (a starting day),
// END_DATE (and end day,
// DATE_RANGE (two days),
// DATE_SET (a set of date ranges),
// IGNORE (ignore this column);
const SELECT_OPTIONS = [
  { label: "ID", value: "ID" },
  { label: "DATE_SET", value: "DATE_SET" },
  { label: "START_DATE", value: "START_DATE" },
  { label: "END_DATE", value: "END_DATE" },
  { label: "IGNORE", value: "IGNORE" }
];

export default ({ file, loading, onUpload, onReset }: PropsT) => {
  const [csv, setCSV] = React.useState([]);
  const [csvHeader, setCSVHeader] = React.useState([]);

  React.useEffect(() => {
    async function parse(f) {
      const result = await parseCSV(f);

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
    }

    parse(file);
  }, [file]);

  function uploadQuery() {
    onUpload({
      format: csvHeader,
      values: csv
    });
  }

  return (
    <div>
      <Row>
        <FileName>{file.name}</FileName>
        <SxIconButton frame regular icon="trash-alt" onClick={onReset} />
      </Row>
      <Table>
        <thead>
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
      <PrimaryButton disabled={loading} onClick={uploadQuery}>
        {loading && <FaIcon white icon="spinner" />}{" "}
        {T.translate("uploadQueryResultsModal.upload")}
      </PrimaryButton>
    </div>
  );
};
