import styled from "@emotion/styled";
import { useRef, FC } from "react";
import { useDrag } from "react-dnd";
import { useTranslation } from "react-i18next";

import { getWidthAndHeight } from "../app/DndProvider";
import IconButton from "../button/IconButton";
import { DNDType } from "../common/constants/dndTypes";
import { DragItemQuery } from "../standard-query-editor/types";
import VerticalToggleButton, {
  Option,
} from "../ui-components/VerticalToggleButton";

import { TimebasedResultType } from "./reducer";

const TimebasedNodeContainer = styled("div")`
  border: 1px solid ${({ theme }) => theme.col.blueGray};
  border-radius: ${({ theme }) => theme.borderRadius};
  transition: all ${({ theme }) => theme.transitionTime};
  overflow: hidden;

  &:hover {
    border: 1px solid ${({ theme }) => theme.col.blueGrayDark};
  }
`;

const TimebasedNodeContent = styled("div")`
  display: table;
  width: 100%;
  background-color: white;
`;

const TimebasedNodeDescription = styled("div")`
  display: table-cell;
  vertical-align: middle;
  position: relative;
  padding: 10px;
`;
const TimebasedNodeDescriptionText = styled("p")`
  word-break: break-word;
`;

const TimebasedNodeTimestamp = styled("div")`
  width: 70px;
  display: table-cell;
  vertical-align: middle;
  border-right: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  text-align: right;
`;

const TimebasedNodeTimestampTitle = styled("p")`
  font-size: ${({ theme }) => theme.font.tiny};
  margin: 0 0 5px;
  padding: 2px 5px 0;
  text-transform: uppercase;
  font-weight: 700;
  color: ${({ theme }) => theme.col.gray};
`;

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
  z-index: 1;
`;

const Root = styled("div")`
  margin: 0 5px;
  width: 200px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const StyledVerticalToggleButton = styled(VerticalToggleButton)`
  ${Option} {
    border: 0;

    &:first-of-type,
    &:last-of-type {
      border-radius: 0;
    }
  }
`;

interface PropsT {
  node: TimebasedResultType;
  position: "left" | "right";
  onRemove: () => void;
  onSetTimebasedNodeTimestamp: (value: string) => void;
  conditionIdx: number;
  resultIdx: number;
}

const TimebasedNode: FC<PropsT> = ({
  node,
  onRemove,
  onSetTimebasedNodeTimestamp,
  conditionIdx,
  resultIdx,
}) => {
  const { t } = useTranslation();
  const ref = useRef<HTMLDivElement | null>(null);
  const item: DragItemQuery = {
    ...node,
    type: DNDType.PREVIOUS_QUERY,
    tags: [],
    dragContext: {
      width: 0,
      height: 0,
      movedFromAndIdx: conditionIdx,
      movedFromOrIdx: resultIdx,
    },
  };
  const [, drag] = useDrag({
    item,
    begin: () => ({
      ...item,
      ...getWidthAndHeight(ref),
    }),
  });

  const toggleButton = (
    <StyledVerticalToggleButton
      onToggle={onSetTimebasedNodeTimestamp}
      activeValue={node.timestamp}
      options={[
        {
          label: t("timebasedQueryEditor.timestampFirst"),
          value: "EARLIEST",
        },
        {
          label: t("timebasedQueryEditor.timestampRandom"),
          value: "RANDOM",
        },
        {
          label: t("timebasedQueryEditor.timestampLast"),
          value: "LATEST",
        },
      ]}
    />
  );

  return (
    <Root
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
    >
      <TimebasedNodeContainer>
        <TimebasedNodeContent>
          <TimebasedNodeTimestamp>
            <TimebasedNodeTimestampTitle>
              {t("timebasedQueryEditor.timestamp")}
            </TimebasedNodeTimestampTitle>
            {toggleButton}
          </TimebasedNodeTimestamp>
          <TimebasedNodeDescription>
            <StyledIconButton icon="times" onClick={onRemove} />
            <TimebasedNodeDescriptionText>
              {node.label || node.id}
            </TimebasedNodeDescriptionText>
          </TimebasedNodeDescription>
        </TimebasedNodeContent>
      </TimebasedNodeContainer>
    </Root>
  );
};

// &__index-result-btn
//   border: none
//   border-top: 1px solid $col-blue-gray-light
//   width: 100%
//   color: $col-black
//   padding: 6px 0
//   background-color: white
//   transition: all $transition-time
//   font-size: $font-xs

//   &:hover
//     &:not(.timebased-node__index-result-btn--active)
//       background-color: $col-gray-very-light

//   &--active
//     background-color: $col-blue-gray-very-light
//     border-top: 1px solid $col-blue-gray
//     color: $col-black

//   &--disabled
//     cursor: not-allowed
//     opacity: 0.6
// Button indexResult (to re-enable this soon)
// <button
//   className={classnames("timebased-node__index-result-btn", {
//     "timebased-node__index-result-btn--active": isIndexResult,
//     "timebased-node__index-result-btn--disabled": isIndexResultDisabled
//   })}
//   disabled={isIndexResultDisabled}
//   onClick={onSetTimebasedIndexResult}
// >
//   {t("timebasedQueryEditor.timestampResultsFrom")}
// </button>

export default TimebasedNode;
