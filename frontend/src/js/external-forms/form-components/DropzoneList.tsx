import { faTimes } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode, Ref } from "react";
import type { DropTargetMonitor } from "react-dnd";
import { tv } from "tailwind-variants";

import IconButton from "../../button/IconButton";
import type {
  ChildArgs,
  PossibleDroppableObject,
} from "../../ui-components/Dropzone";
import DropzoneWithFileInput, {
  type DragItemFile,
} from "../../ui-components/DropzoneWithFileInput";
import InfoTooltip from "../../ui-components/InfoTooltip";
import Label from "../../ui-components/Label";

import DropzoneBetweenElements from "./DropzoneBetweenElements";

const listItem = tv({
  base: [
    "relative",
    "p-[5px]",
    "mb-[5px]",
    "bg-white",
    "rounded",
    "shadow-[0_0_3px_0_rgba(0,0,0,0.1)]",
  ],
});

const betweenDropzone = tv({
  variants: {
    first: { true: "top-[3px]" },
  },
});

const lastBetweenDropzone = tv({
  base: ["-top-[5px]", "h-[15px]"],
});

interface PropsT<DroppableObject> {
  className?: string;
  label?: ReactNode;
  tooltip?: string;
  dropzoneChildren: (args: ChildArgs<DroppableObject>) => ReactNode;
  items: ReactNode[];
  acceptedDropTypes: string[];
  onDelete: (idx: number) => void;
  disallowMultipleColumns?: boolean;
  onDrop: (
    props: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  onDropFile: (file: File) => void;
  onImportLines: (lines: string[], filename?: string) => void;
  dropBetween: (
    i: number,
  ) => (item: PossibleDroppableObject, monitor: DropTargetMonitor) => void;
}

const DropzoneList = <DroppableObject extends PossibleDroppableObject>({
  className,
  label,
  tooltip,
  dropzoneChildren,
  items,
  acceptedDropTypes,
  onDelete,
  disallowMultipleColumns,
  onDrop,
  onImportLines,
  dropBetween,
  ref,
}: PropsT<DroppableObject> & { ref?: Ref<HTMLDivElement> }) => {
  // allow at least one column
  const showDropzone =
    (items && items.length === 0) || !disallowMultipleColumns;

  return (
    <div className={className}>
      <div className="flex items-center">
        {label && <Label>{label}</Label>}
        {tooltip && <InfoTooltip text={tooltip} />}
      </div>
      {items && items.length > 0 && (
        <>
          {items.map((item, i) => (
            <div className="relative" key={i}>
              {!disallowMultipleColumns && (
                <DropzoneBetweenElements
                  className={betweenDropzone({ first: i === 0 })}
                  acceptedDropTypes={acceptedDropTypes}
                  onDrop={dropBetween(i)}
                />
              )}
              <div className={listItem()}>
                <IconButton
                  className="absolute top-0 right-0"
                  bgHover
                  icon={faTimes}
                  onClick={() => onDelete(i)}
                />
                {item}
              </div>
            </div>
          ))}
          <div className="relative">
            {!disallowMultipleColumns && (
              <DropzoneBetweenElements
                className={lastBetweenDropzone()}
                acceptedDropTypes={acceptedDropTypes}
                onDrop={dropBetween(items.length)}
              />
            )}
          </div>
        </>
      )}
      <div ref={ref}>
        {showDropzone && onImportLines && (
          <DropzoneWithFileInput
            acceptedDropTypes={acceptedDropTypes}
            onDrop={onDrop}
            onImportLines={onImportLines}
          >
            {dropzoneChildren}
          </DropzoneWithFileInput>
        )}
      </div>
    </div>
  );
};

export default DropzoneList;
