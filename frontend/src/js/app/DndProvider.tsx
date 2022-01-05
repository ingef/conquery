import styled from "@emotion/styled";
import { FC } from "react";
import { DndProvider as ReactDndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import MultiBackend, {
  TouchTransition,
  usePreview,
} from "react-dnd-multi-backend";
import { TouchBackend } from "react-dnd-touch-backend";

import { DNDType } from "../common/constants/dndTypes";
import { PossibleDroppableObject } from "../ui-components/Dropzone";

const PreviewItem = styled("div")<{ width: number; height: number }>`
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  opacity: 0.9;
  box-shadow: 0 0 15px 0 rgba(0, 0, 0, 0.2);
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.gray};
  width: ${({ width }) => `${width}px`};
  height: ${({ height }) => `${height}px`};
`;

const CustomHTML5toTouch = {
  backends: [
    {
      backend: HTML5Backend,
    },
    {
      backend: TouchBackend,
      transition: TouchTransition,
      options: { enableMouseEvents: true, delayTouchStart: 100 },
      preview: true,
    },
  ],
};

// Helper function to calculate Touch backend preview width and height
// To use on begin drag
export function getWidthAndHeight(ref: React.RefObject<HTMLElement | null>) {
  const rect = ref.current?.getBoundingClientRect();

  return {
    width: rect?.width || 0,
    height: rect?.height || 0,
  };
}

const findItemWithAndHeight = (
  item: PossibleDroppableObject,
): { width: number; height: number } => {
  switch (item.type) {
    case "__NATIVE_FILE__":
      return { width: 0, height: 0 };
    case DNDType.FORM_CONFIG:
    case DNDType.CONCEPT_TREE_NODE:
    case DNDType.PREVIOUS_QUERY:
    case DNDType.PREVIOUS_SECONDARY_ID_QUERY:
      return { width: item.dragContext.width, height: item.dragContext.height };
  }
};

const DndPreview: FC = () => {
  const { display, item, style } = usePreview();

  if (!display) {
    return null;
  }

  const { width, height } = findItemWithAndHeight(item);

  return <PreviewItem width={width} height={height} style={style} />;
};

const DndProvider: FC = ({ children }) => {
  return (
    <ReactDndProvider backend={MultiBackend} options={CustomHTML5toTouch}>
      {children}
      <DndPreview />
    </ReactDndProvider>
  );
};

export default DndProvider;
