import styled from "@emotion/styled";
import { PreviewStatistics } from "../api/types";
import Modal from "../modal/Modal";
import Diagram from "./Diagram";
import { formatNumber, previewStatsIsNumberStats } from "./util";
import { StyledTable } from "./Table";
import RcTable from "rc-table";
import { t } from "i18next";

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
        {previewStatsIsNumberStats(statistic) && (
          <RcTable columns={
            [
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
            ]
          }
            data={
              [
                {
                  "name": t("common.min"),
                  "value": formatNumber(statistic.min)
                },
                {
                  "name": t("common.max"),
                  "value": formatNumber(statistic.max)
                },
                {
                  "name": t("common.average"),
                  "value": formatNumber(statistic.mean)
                },
                {
                  "name": t("common.std"),
                  "value": formatNumber(statistic.stdDev)
                },
              ]
            }
            rowKey={(_, index) => `row_${index}`}
            components={components}
          />
        )}
      </Horizontal>
    </Modal>
  );
}
