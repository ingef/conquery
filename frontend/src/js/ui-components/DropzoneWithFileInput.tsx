import styled from "@emotion/styled";
import { useRef, ReactNode, Ref, forwardRef, ReactElement } from "react";
import { DropTargetMonitor } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";

import { SelectFileButton } from "../button/SelectFileButton";
import FaIcon from "../icon/FaIcon";

import Dropzone, { ChildArgs, PossibleDroppableObject } from "./Dropzone";

export interface DragItemFile {
  type: "__NATIVE_FILE__"; // Actually, this seems to not be passed by react-dnd
  files: File[];
}

const FileInput = styled("input")`
  display: none;
`;

const SxDropzone = styled(Dropzone)<{ isInitial?: boolean }>`
  cursor: ${({ isInitial }) => (isInitial ? "initial" : "pointer")};
  transition: box-shadow ${({ theme }) => theme.transitionTime};
  position: relative;

  &:hover {
    box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  }
`;

const SxSelectFileButton = styled(SelectFileButton)`
  position: absolute;
  top: 5px;
  right: 10px;
`;

const SxFaIcon = styled(FaIcon)`
  height: 10px;
  padding-right: 3px;
`;

interface PropsT<DroppableObject> {
  children: (args: ChildArgs<DroppableObject>) => ReactNode;
  onSelectFile: (file: File) => void;
  onDrop: (
    item: DroppableObject | DragItemFile,
    monitor: DropTargetMonitor,
  ) => void;
  acceptedDropTypes?: string[];
  accept?: string;
  disableClick?: boolean;
  showFileSelectButton?: boolean;
  isInitial?: boolean;
  className?: string;
}

/*
  Augments a dropzone with file drop support

  - opens file dialog on dropzone click
  - adds NativeTypes.FILE

  => The "onDrop"-prop needs to handle the file drop itself, though!
*/
const DropzoneWithFileInput = <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  {
    onSelectFile,
    acceptedDropTypes,
    disableClick,
    showFileSelectButton,
    children,
    onDrop,
    isInitial,
    className,
    accept,
  }: PropsT<DroppableObject>,
  ref: Ref<HTMLDivElement>,
) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const dropTypes = [...(acceptedDropTypes || []), NativeTypes.FILE];

  function onOpenFileDialog() {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }

  return (
    <SxDropzone /* <FC<DropzoneProps<DroppableObject | DragItemFile>>> */
      acceptedDropTypes={dropTypes}
      onClick={() => {
        if (disableClick) return;

        onOpenFileDialog();
      }}
      onDrop={(item, monitor) => {
        if ("files" in item) {
          // Because it doesn't seem to be added by react-dnd
          item.type = NativeTypes.FILE;
        }

        onDrop(item as DroppableObject | DragItemFile, monitor);
      }}
      isInitial={isInitial}
      className={className}
      ref={ref}
    >
      {(args) => (
        <>
          {showFileSelectButton && (
            <SxSelectFileButton onClick={onOpenFileDialog}>
              <SxFaIcon icon="file" regular gray />
              {t("inputMultiSelect.openFileDialog")}
            </SxSelectFileButton>
          )}
          <FileInput
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
          {children(args as ChildArgs<DroppableObject>)}
        </>
      )}
    </SxDropzone>
  );
};

export default forwardRef(DropzoneWithFileInput) as <
  DroppableObject extends PossibleDroppableObject = DragItemFile,
>(
  p: PropsT<DroppableObject> & { ref?: Ref<HTMLDivElement> },
) => ReactElement;
