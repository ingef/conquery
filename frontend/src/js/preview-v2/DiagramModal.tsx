import styled from "@emotion/styled";
import { PreviewStatistics } from "../api/types";
import Modal from "../modal/Modal";
import Diagram from "./Diagram";
import { previewStatsIsNumberStats } from "./util";

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

const SxTable = styled("table")`
  border: 1px solid black;
  align-self: center;
  margin-left: 15px;
`;

export default function DiagramModal({
  statistic,
  onClose,
}: DiagramModalProps) {
  return (
    <Modal closeIcon onClose={() => onClose()}>
      <Horizontal>
        <SxDiagram stat={statistic} />
        {
          previewStatsIsNumberStats(statistic) && (
            <SxTable border={1}>
              <tr>
                <td>Mean</td>
                <td>{statistic.mean.toPrecision(3)}</td>
              </tr>
              <tr>
                <td>Standard Deviation</td>
                <td>{statistic.stdDev.toPrecision(3)}</td>
              </tr>
              <tr>
                <td>Minimum</td>
                <td>{statistic.min.toPrecision(3)}</td>
              </tr>
              <tr>
                <td>Maximum</td>
                <td>{statistic.max.toPrecision(3)}</td>
              </tr>
            </SxTable>
          )
        }
      </Horizontal>
    </Modal>
  );
}
