import { useCallback, useRef } from "react";

import { DNDType } from "../../common/constants/dndTypes";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";
import Dropzone from "../../ui-components/Dropzone";
import InfoTooltip from "../../ui-components/InfoTooltip";
import Label from "../../ui-components/Label";

import ValidatedFormQueryResult from "./ValidatedFormQueryResult";

const DROP_TYPES = [
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const FormQueryDropzone = ({
  label,
  tooltip,
  dropzoneText,
  className,
  value,
  onChange,
}: {
  label: string;
  tooltip?: string;
  dropzoneText: string;
  className?: string;
  value: DragItemQuery | null;
  onChange: (value: DragItemQuery | null) => void;
}) => {
  const onDrop = (item: DragItemQuery) => {
    onChange(item);
  };

  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  const onInvalid = useCallback(() => {
    // It would be better to call `setError` to register an error for the field,
    // but that error won't persist when another `useController` call is made for that field
    // during field registration, so we have to do something here that
    // makes the field not pass the `validate` rule.
    onChangeRef.current(null);
  }, []);
  const onDelete = useCallback(() => {
    onChangeRef.current(null);
  }, []);

  return (
    <div className={className}>
      <Label>
        {label}
        {exists(tooltip) && <InfoTooltip text={tooltip} />}
      </Label>
      <Dropzone
        className="justify-start"
        onDrop={(item) => onDrop(item as DragItemQuery)}
        acceptedDropTypes={DROP_TYPES}
      >
        {() => (
          <ValidatedFormQueryResult
            placeholder={dropzoneText}
            queryResult={value || undefined}
            onInvalid={onInvalid}
            onDelete={onDelete}
          />
        )}
      </Dropzone>
    </div>
  );
};

export default FormQueryDropzone;
