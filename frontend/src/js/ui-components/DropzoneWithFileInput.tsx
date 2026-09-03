import { type ReactNode, type Ref, useRef, useState } from "react";
import type { DropTargetMonitor } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "./Button";

import Dropzone, {
  type ChildArgs,
  type PossibleDroppableObject,
} from "./Dropzone";
import { ImportModal } from "./ImportModal";

export interface DragItemFile {
  type: "__NATIVE_FILE__"; // Actually, this seems to not be passed by react-dnd
  files: File[];
}

const dropzone = tv({
  base: [
    "relative",
    "cursor-pointer",
    "transition-shadow duration-100",
    "hover:shadow-[0_0_5px_0_rgba(0,0,0,0.2)]",
  ],
  variants: {
    isInitial: { true: "cursor-[initial]" },
    tight: { true: "p-[5px]" },
  },
});

// a small text link at the dropzone's top right corner, or above it
const importButton = tv({
  base: "absolute text-xs",
  variants: {
    outside: {
      true: "-top-[30px] right-0",
      false: "top-[3px] right-2",
    },
  },
});

interface PropsT<DroppableObject> {
  children: (args: ChildArgs<DroppableObject>) => ReactNode;
  onSelectFile?: (file: File) => void;
  onDrop: (
    item: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  acceptedDropTypes?: string[];
  accept?: string;
  disableClick?: boolean;
  isInitial?: boolean;
  className?: string;
  tight?: boolean;

  showImportButton?: boolean;
  importButtonOutside?: boolean;
  onImportLines?: (lines: string[], filename?: string) => void;
  importPlaceholder?: string;
  importDescription?: string;
}

/*
  Augments a dropzone with file drop support

  - opens file dialog on dropzone click
  - adds NativeTypes.FILE

  => The "onDrop"-prop needs to handle the file drop itself, though!
*/
const DropzoneWithFileInput = <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>({
  onSelectFile,
  onImportLines,
  importPlaceholder,
  importDescription,
  importButtonOutside,
  acceptedDropTypes,
  disableClick,
  showImportButton,
  children,
  onDrop,
  isInitial,
  className,
  accept,
  tight,
  ref,
}: PropsT<DroppableObject> & { ref?: Ref<HTMLDivElement> }) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const dropTypes = [...(acceptedDropTypes || []), NativeTypes.FILE];

  const [importModalOpen, setImportModalOpen] = useState(false);

  function onSubmitImport(lines: string[], filename?: string) {
    onImportLines?.(lines, filename);
  }

  function onOpenFileDialog() {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }

  return (
    <Dropzone
      acceptedDropTypes={dropTypes}
      onClick={() => {
        if (disableClick) return;

        if (onImportLines) {
          setImportModalOpen(true);
        } else {
          onOpenFileDialog();
        }
      }}
      onDrop={(item, monitor) => {
        if ("files" in item) {
          // Because it doesn't seem to be added by react-dnd
          item.type = NativeTypes.FILE;
        }

        onDrop(item as DroppableObject | DragItemFile, monitor);
      }}
      className={dropzone({ isInitial, tight, className })}
      ref={ref}
    >
      {(args) => (
        <>
          {importModalOpen && (
            <ImportModal
              onClose={() => setImportModalOpen(false)}
              onSubmit={onSubmitImport}
              placeholder={importPlaceholder}
              description={importDescription}
            />
          )}
          {showImportButton && onImportLines && (
            <Button
              intent="link"
              className={importButton({ outside: !!importButtonOutside })}
              onPress={() => setImportModalOpen(true)}
            >
              {t("common.import")}
            </Button>
          )}
          {onSelectFile && (
            <input
              className="hidden"
              ref={fileInputRef}
              type="file"
              accept={accept}
              onChange={(e) => {
                if (e.target.files) {
                  onSelectFile(e.target.files[0]);
                }

                if (fileInputRef.current) {
                  fileInputRef.current.value = "";
                }
              }}
            />
          )}
          {children(args as ChildArgs<DroppableObject>)}
        </>
      )}
    </Dropzone>
  );
};

export default DropzoneWithFileInput;
