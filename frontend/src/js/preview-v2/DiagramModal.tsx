import styled from "@emotion/styled";
import { t } from "i18next";
import RcTable from "rc-table";
import { PreviewStatistics } from "../api/types";
import Modal from "../modal/Modal";
import Diagram from "./Diagram";
import { StyledTable } from "./Table";
import { previewStatsIsBarStats } from "./util";

interface DiagramModalProps {
  statistic: PreviewStatistics;
  onClose: () => void;
}

const Horizontal = styled("div")`
  display: inline-flex;
`;

const SxDiagram = styled(Diagram)`
  width: 70vw;
  height: 70vh;
  margin-right: 15px;
`;

const StyledRcTable = styled(RcTable)`
  margin: auto;
`;

export default function DiagramModal({
  statistic,
  onClose,
}: DiagramModalProps) {
  const components = {
    table: StyledTable,
  };
  return (
    <Modal closeIcon onClose={() => onClose()}>
      <Horizontal>
        <SxDiagram stat={statistic} />
        {previewStatsIsBarStats(statistic) &&
          Object.keys(statistic.extras).length > 0 && (
            <StyledRcTable
              columns={[
                {
                  title: t("preview.name"),
                  dataIndex: "name",
                  key: "name",
                },
                {
                  title: t("preview.value"),
                  dataIndex: "value",
                  key: "value",
                },
              ]}
              data={Object.entries(statistic.extras).map(([name, value]) => {
                return { name, value };
              })}
              rowKey={(_, index) => `row_${index}`}
              components={components}
            />
          )}
      </Horizontal>
    </Modal>
  );
}
