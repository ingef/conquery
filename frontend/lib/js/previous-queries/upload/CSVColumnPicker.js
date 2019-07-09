// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import parseCSV from "../../file/parseCSV";

import PrimaryButton from "../../button/PrimaryButton";
import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";

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

const SxIconButton = styled(IconButton)`
  margin-left: 10px;
`;

const FileName = styled("p")`
  margin: 0;
`;

function buildQuery(csv) {
  // This is experimental still.
  // External queries (uploaded lists) may contain three or four columns.
  // The first two columns are IDs, which will be concatenated
  // The other two columns are date ranges
  // We simply assume that the data is in this format
  // Will produce upload errors when the data has a different format
  //
  // Based on the possible:
  // ID (some string)
  // EVENT_DATE (a single day),
  // START_DATE (a starting day),
  // END_DATE (and end day,
  // DATE_RANGE (two days),
  // DATE_SET (a set of date ranges),
  // IGNORE (ignore this column);

  return {
    format:
      csv.data[0].length >= 4
        ? ["ID", "ID", "START_DATE", "END_DATE"]
        : ["ID", "ID", "DATE_SET"],
    values: csv.data
  };
}

export default ({ file, loading, onUpload, onReset }: PropsT) => {
  const [csv, setCSV] = React.useState([]);

  React.useEffect(() => {
    async function parse(f) {
      const result = await parseCSV(f);

      setCSV(result);
    }

    parse(file);
  }, [file]);

  function uploadQuery() {
    onUpload(buildQuery(csv));
  }

  return (
    <div>
      <Row>
        <FileName>{file.name}</FileName>
        <SxIconButton frame regular icon="trash-alt" onClick={onReset} />
      </Row>
      <PrimaryButton disabled={loading} onClick={uploadQuery}>
        {loading && <FaIcon white icon="spinner" />}{" "}
        {T.translate("uploadQueryResultsModal.upload")}
      </PrimaryButton>
    </div>
  );
};
