import styled from "@emotion/styled";
import { FC } from "react";
import { DndProvider as ReactDndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import MultiBackend, {
  TouchTransition,
  usePreview,
} from "react-dnd-multi-backend";
import { TouchBackend } from "react-dnd-touch-backend";

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

const DndPreview: FC = () => {
  const { display, item, style } = usePreview();

  if (!display) {
    return null;
  }

  return <PreviewItem width={item.width} height={item.height} style={style} />;
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
