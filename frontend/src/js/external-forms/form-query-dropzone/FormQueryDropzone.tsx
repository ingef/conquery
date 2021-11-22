import styled from "@emotion/styled";
import { FC } from "react";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Dropzone from "../../ui-components/Dropzone";
import Label from "../../ui-components/Label";
import Optional from "../../ui-components/Optional";

import FormQueryResult from "./FormQueryResult";

const SxDropzone = styled(Dropzone)<{ centered?: boolean }>`
  justify-content: ${({ centered }) => (centered ? "center" : "flex-start")};
`;

interface PropsT {
  label: string;
  tooltip?: string;
  optional?: boolean;
  dropzoneText: string;
  className?: string;
  value: DragItemQuery | null;
  onChange: (value: DragItemQuery | null) => void;
}

const FormQueryDropzone: FC<PropsT> = ({
  label,
  tooltip,
  optional,
  dropzoneText,
  className,
  value,
  onChange,
}) => {
  const onDrop = (item: DragItemQuery) => {
    onChange(item);
  };

  return (
    <div className={className}>
      <Label>
        {optional && <Optional />}
        {label}
        {exists(tooltip) && <InfoTooltip text={tooltip} />}
      </Label>
      <SxDropzone<FC<DropzoneProps<DragItemQuery>>>
        onDrop={onDrop}
        acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
        centered={!value}
      >
        {() =>
          !value ? (
            dropzoneText
          ) : (
            <FormQueryResult
              queryResult={value}
              onDelete={() => onChange(null)}
            />
          )
        }
      </SxDropzone>
    </div>
  );
};

export default FormQueryDropzone;
