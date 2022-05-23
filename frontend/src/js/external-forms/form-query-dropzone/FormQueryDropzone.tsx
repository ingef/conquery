import styled from "@emotion/styled";
import { FC } from "react";

import { DNDType } from "../../common/constants/dndTypes";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";
import InfoTooltip from "../../tooltip/InfoTooltip";
import Dropzone from "../../ui-components/Dropzone";
import Label from "../../ui-components/Label";
import Optional from "../../ui-components/Optional";

import ValidatedFormQueryResult from "./ValidatedFormQueryResult";

const SxDropzone = styled(Dropzone)`
  justify-content: flex-start;
`;

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

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

  const onInvalid = () => {
    // It would be better to call `setError` to register an error for the field,
    // but that error won't persist when another `useController` call is made for that field
    // during field registration, so we have to do something here that
    // makes the field not pass the `validate` rule.
    onChange(null);
  };

  return (
    <div className={className}>
      <Label>
        {optional && <Optional />}
        {label}
        {exists(tooltip) && <InfoTooltip text={tooltip} />}
      </Label>
      <SxDropzone /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<DragItemQuery>>> */
        onDrop={(item) => onDrop(item as DragItemQuery)}
        acceptedDropTypes={DROP_TYPES}
      >
        {() => (
          <ValidatedFormQueryResult
            placeholder={dropzoneText}
            queryResult={value || undefined}
            onInvalid={onInvalid}
            onDelete={() => onChange(null)}
          />
        )}
      </SxDropzone>
    </div>
  );
};

export default FormQueryDropzone;
