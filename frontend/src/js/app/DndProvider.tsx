import type { ReactNode } from "react";
import { DndProvider as ReactDndProvider } from "react-dnd";
import { HTML5Backend } from "react-dnd-html5-backend";
import {
  MultiBackend,
  TouchTransition,
  usePreview,
} from "react-dnd-multi-backend";
import { TouchBackend } from "react-dnd-touch-backend";
import { tv } from "tailwind-variants";

import { DNDType } from "../common/constants/dndTypes";
import type { PossibleDroppableObject } from "../ui-components/Dropzone";

const previewItem = tv({
  base: [
    "rounded",
    "border border-gray-500",
    "bg-gray-50",
    "opacity-90",
    "shadow-[0_0_15px_0_rgba(0,0,0,0.2)]",
  ],
});

const CustomHTML5toTouch = {
  backends: [
    {
      id: "html5",
      backend: HTML5Backend,
    },
    {
      id: "touch",
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

const DndPreview = () => {
  const preview = usePreview<PossibleDroppableObject>();

  if (!preview.display) {
    return null;
  }

  const { width, height } = findItemWithAndHeight(preview.item);

  return (
    <div
      className={previewItem()}
      style={{ width, height, ...preview.style }}
    />
  );
};

const DndProvider = ({ children }: { children: ReactNode }) => {
  return (
    <ReactDndProvider backend={MultiBackend} options={CustomHTML5toTouch}>
      {children}
      <DndPreview />
    </ReactDndProvider>
  );
};

export default DndProvider;
