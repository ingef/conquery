// @flow

import * as React from "react";
import styled from "@emotion/styled";
import { NativeTypes } from "react-dnd-html5-backend";

import Dropzone from "./Dropzone";

const FileInput = styled("input")`
  display: none;
`;

const SxDropzone = styled(Dropzone)`
  cursor: pointer;
  transition: box-shadow ${({ theme }) => theme.transitionTime};

  &:hover {
    box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  }
`;

type PropsT = {
  children: React.Node,
  acceptedDropTypes?: string[],
  onSelectFile: File => void
};

export default ({
  children,
  onSelectFile,
  acceptedDropTypes,
  ...props
}: PropsT) => {
  const fileInputRef = React.useRef(null);

  const dropTypes = [...(acceptedDropTypes || []), NativeTypes.FILE];

  function onOpenFileDialog() {
    fileInputRef.current.click();
  }

  return (
    <SxDropzone
      acceptedDropTypes={dropTypes}
      onClick={onOpenFileDialog}
      {...props}
    >
      <FileInput
        ref={fileInputRef}
        type="file"
        onChange={e => onSelectFile(e.target.files[0])}
      />
      {children}
    </SxDropzone>
  );
};
