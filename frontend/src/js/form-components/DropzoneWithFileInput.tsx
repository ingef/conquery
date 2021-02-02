import React, { FC, useRef } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { NativeTypes } from "react-dnd-html5-backend";

import Dropzone, { ChildArgs } from "./Dropzone";
import { DropTargetMonitor } from "react-dnd";

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

const TopRight = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.tiny};
  color: ${({ theme }) => theme.col.gray};
  position: absolute;
  top: 5px;
  right: 10px;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

interface PropsT {
  children: (args: ChildArgs) => React.ReactNode;
  onSelectFile: (file: File) => void;
  onDrop: (props: any, monitor: DropTargetMonitor) => void;
  acceptedDropTypes?: string[];
  disableClick?: boolean;
  showFileSelectButton?: boolean;
  isInitial?: boolean;
}

/*
  Augments a dropzone with file drop support

  - opens file dialog on dropzone click
  - adds NativeTypes.FILE

  => The "onDrop"-prop needs to handle the file drop itself, though!
*/
const DropzoneWithFileInput: FC<PropsT> = ({
  onSelectFile,
  acceptedDropTypes,
  disableClick,
  showFileSelectButton,
  children,
  ...props
}) => {
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const dropTypes = [...(acceptedDropTypes || []), NativeTypes.FILE];

  function onOpenFileDialog() {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }

  return (
    <SxDropzone
      acceptedDropTypes={dropTypes}
      onClick={() => {
        if (disableClick) return;

        onOpenFileDialog();
      }}
      {...props}
    >
      {(args: ChildArgs) => (
        <>
          {showFileSelectButton && (
            <TopRight onClick={onOpenFileDialog}>
              {T.translate("inputMultiSelect.openFileDialog")}
            </TopRight>
          )}
          <FileInput
            ref={fileInputRef}
            type="file"
            onChange={(e) => {
              if (e.target.files) {
                onSelectFile(e.target.files[0]);
              }

              if (fileInputRef.current) {
                fileInputRef.current.value = null;
              }
            }}
          />
          {children(args)}
        </>
      )}
    </SxDropzone>
  );
};

export default DropzoneWithFileInput;
