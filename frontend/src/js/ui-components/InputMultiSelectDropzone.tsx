import styled from "@emotion/styled";
import { FC, ReactNode, useRef } from "react";
import { NativeTypes } from "react-dnd-html5-backend";
import { useTranslation } from "react-i18next";

import { SelectFileButton } from "../button/SelectFileButton";
import FaIcon from "../icon/FaIcon";

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

const SxSelectFileButton = styled(SelectFileButton)`
  position: absolute;
  top: -15px;
  right: 0;
`;

const SxFaIcon = styled(FaIcon)`
  height: 10px;
  padding-right: 3px;
`;

interface PropsT {
  className?: string;
  disabled?: boolean;
  onDropFile: (file: File) => void;
  children: () => ReactNode;
}

const InputMultiSelectDropzone: FC<PropsT> = ({
  className,
  disabled,
  onDropFile,
  children,
}) => {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  function onOpenFileDialog() {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  }

  return (
    <Root className={className}>
      <SxDropzone /* TODO: ADD GENERIC TYPE <DragItemFile> */
        acceptedDropTypes={[NativeTypes.FILE]}
        onDrop={(item) => {
          onDropFile((item as DragItemFile).files[0]);
        }}
      >
        {children}
      </SxDropzone>
      <SxSelectFileButton bare disabled={disabled} onClick={onOpenFileDialog}>
        <SxFaIcon icon="file" regular gray />
        {t("inputMultiSelect.openFileDialog")}
        <FileInput
          ref={fileInputRef}
          type="file"
          onChange={(e) => {
            if (e.target.files) {
              onDropFile(e.target.files[0]);
              if (fileInputRef.current) {
                fileInputRef.current.value = "";
              }
            }
          }}
        />
      </SxSelectFileButton>
    </Root>
  );
};

export default InputMultiSelectDropzone;
