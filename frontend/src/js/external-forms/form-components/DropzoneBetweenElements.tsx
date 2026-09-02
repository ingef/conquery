import type { DropTargetMonitor } from "react-dnd";
import { tv } from "tailwind-variants";

import Dropzone, {
  type PossibleDroppableObject,
} from "../../ui-components/Dropzone";

// line height 4px; the dropzone's translate offsets by half of it (2px) —
// written literally because tailwind only generates classes it finds in source
const line = tv({
  base: ["h-[4px]", "w-full", "rounded", "bg-primary-500"],
});

const dropzone = tv({
  base: [
    "absolute top-0 left-0",
    "z-1",
    "h-[30px]",
    "translate-y-[calc(-50%_-_2px)]",
    "bg-transparent",
  ],
});

const DropzoneBetweenElements = ({
  acceptedDropTypes,
  onDrop,
  className,
}: {
  onDrop: (props: PossibleDroppableObject, monitor: DropTargetMonitor) => void;
  acceptedDropTypes: string[];
  className?: string;
}) => {
  return (
    <Dropzone
      className={dropzone({ className })}
      bare
      naked
      acceptedDropTypes={acceptedDropTypes}
      onDrop={onDrop}
    >
      {({ isOver }) => isOver && <div className={line()} />}
    </Dropzone>
  );
};

export default DropzoneBetweenElements;
