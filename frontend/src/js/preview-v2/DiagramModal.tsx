import styled from "@emotion/styled";
import { PreviewStatistics } from "../api/types";
import Modal from "../modal/Modal";
import Diagram from "./Diagram";

interface DiagramModalProps { 
    statistic: PreviewStatistics;
    onClose: () => void;
}

const Grid = styled("div")`
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    grid-gap: 5px;
`;

const SxDiagram = styled(Diagram)`
    width: 80%;
`;


export default function DiagramModal({statistic, onClose}: DiagramModalProps) {
    return (
        <Modal
            closeIcon
            onClose={() => onClose()}
        >
            <Grid>
                <SxDiagram stat={statistic}/>
            </Grid>
        </Modal>
    );
}