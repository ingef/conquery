import styled from "@emotion/styled";
import React, { FC } from "react";
import type { WrappedFieldProps } from "redux-form";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import { exists } from "../../common/helpers/exists";
import Dropzone from "../../form-components/Dropzone";
import Label from "../../form-components/Label";
import type { DragItemQuery } from "../../standard-query-editor/types";
import InfoTooltip from "../../tooltip/InfoTooltip";

import FormQueryResult from "./FormQueryResult";

const SxDropzone = styled(Dropzone)<{ centered?: boolean }>`
  justify-content: ${({ centered }) => (centered ? "center" : "flex-start")};
`;

interface PropsT extends WrappedFieldProps {
  label: string;
  tooltip?: string;
  dropzoneText: string;
  className?: string;
}

const FormQueryDropzone: FC<PropsT> = (props) => {
  const onDrop = (item: DragItemQuery) => {
    props.input.onChange(item);
  };

  return (
    <div className={props.className}>
      <Label>
        {props.label}
        {exists(props.tooltip) && <InfoTooltip text={props.tooltip} />}
      </Label>
      <SxDropzone<FC<DropzoneProps<DragItemQuery>>>
        onDrop={onDrop}
        acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
        centered={!props.input.value}
      >
        {() =>
          !props.input.value ? (
            props.dropzoneText
          ) : (
            <FormQueryResult
              queryResult={props.input.value}
              onDelete={() => props.input.onChange(null)}
            />
          )
        }
      </SxDropzone>
    </div>
  );
};

export default FormQueryDropzone;
