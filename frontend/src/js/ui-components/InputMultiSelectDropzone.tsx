import styled from "@emotion/styled";
import React, { FC, ReactNode, useRef } from "react";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";

import Dropzone from "./Dropzone";
import type { DragItemFile } from "./DropzoneWithFileInput";

const Root = styled("div")`
  position: relative;
`;

const SxDropzone = styled(Dropzone)`
  padding: 5px;
`;

const FileInput = styled("input")`
  display: none;
`;

const TopRight = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.tiny};
  color: ${({ theme }) => theme.col.gray};
  position: absolute;
  top: -15px;
  right: 0;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

interface PropsT {
  className?: string;
  onDropFile: (file: File) => void;
  children: () => ReactNode;
}

const InputMultiSelectDropzone: FC<PropsT> = ({
  className,
  onDropFile,
  children,
}) => {
  const { t } = useTranslation();
  const fileInputRef = useRef(null);

  function onOpenFileDialog() {
    fileInputRef.current.click();
  }

  return (
    <Root className={className}>
      <SxDropzone<DragItemFile>
        acceptedDropTypes={[NativeTypes.FILE]}
        onDrop={(item) => {
          onDropFile(item.files[0]);
        }}
      >
        {children}
      </SxDropzone>
      <TopRight onClick={onOpenFileDialog}>
        {t("inputMultiSelect.openFileDialog")}
        <FileInput
          ref={fileInputRef}
          type="file"
          onChange={(e) => {
            onDropFile(e.target.files[0]);
            fileInputRef.current.value = null;
          }}
        />
      </TopRight>
    </Root>
  );
};

export default InputMultiSelectDropzone;
