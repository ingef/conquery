import React, { useRef, FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "@emotion/styled";
import { useDrag } from "react-dnd";

import VerticalToggleButton, {
  Option,
} from "../form-components/VerticalToggleButton";
import {
  EARLIEST,
  LATEST,
  RANDOM,
} from "../common/constants/timebasedQueryTimestampTypes";
import { TIMEBASED_NODE } from "../common/constants/dndTypes";
import IconButton from "../button/IconButton";
import { getWidthAndHeight } from "../app/DndProvider";

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
  node: Object;
  position: "left" | "right";
  isIndexResult: boolean;
  onRemove: Function;
  onSetTimebasedNodeTimestamp: Function;
  onSetTimebasedIndexResult: Function;
  conditionIdx: number;
  resultIdx: number;
  connectDragSource: Function;
  isIndexResultDisabled: boolean;
}

const TimebasedNode: FC<PropsT> = ({
  node,
  // isIndexResult,
  // isIndexResultDisabled,
  onRemove,
  // onSetTimebasedIndexResult,
  onSetTimebasedNodeTimestamp,
  conditionIdx,
  resultIdx,
}) => {
  const { t } = useTranslation();
  const ref = useRef<HTMLDivElement | null>(null);
  const item = {
    conditionIdx,
    resultIdx,
    node,
    moved: true,
    type: TIMEBASED_NODE,
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
          value: EARLIEST,
        },
        {
          label: t("timebasedQueryEditor.timestampRandom"),
          value: RANDOM,
        },
        {
          label: t("timebasedQueryEditor.timestampLast"),
          value: LATEST,
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
      <div className="timebased-node__container">
        <div className="timebased-node__content">
          <div className="timebased-node__timestamp">
            <p className="timebased-node__timestamp__title">
              {t("timebasedQueryEditor.timestamp")}
            </p>
            {toggleButton}
          </div>
          <div className="timebased-node__description">
            <StyledIconButton icon="times" onClick={onRemove} />
            <p className="timebased-node__description__text">
              {node.label || node.id}
            </p>
          </div>
        </div>
      </div>
    </Root>
  );
};

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
