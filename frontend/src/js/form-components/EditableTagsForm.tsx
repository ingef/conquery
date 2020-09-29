import React, { FC, useState, useRef, FormEvent } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import PrimaryButton from "../button/PrimaryButton";
import ReactSelect from "./ReactSelect";
import { useClickOutside } from "../common/helpers/useClickOutside";

interface PropsT {
  className?: string;
  tags?: string[];
  loading: boolean;
  onSubmit: (tags: string[]) => void;
  onCancel: () => void;
  availableTags: string[];
}

interface ValueT {
  label: string;
  value: string;
}

const StyledPrimaryButton = styled(PrimaryButton)`
  margin-top: 5px;
`;

const EditableTagsForm: FC<PropsT> = ({
  className,
  tags,
  loading,
  onSubmit,
  onCancel,
  availableTags,
}) => {
  const ref = useRef(null);
  const [values, setValues] = useState<ValueT[]>(
    tags ? tags.map((t) => ({ label: t, value: t })) : []
  );
  useClickOutside(ref, onCancel);

  function submit(e: FormEvent) {
    e.preventDefault();

    onSubmit(values ? values.map((v) => v.value) : []);
  }

  return (
    <form ref={ref} className={className} onSubmit={submit}>
      <ReactSelect
        creatable
        name="input"
        value={values}
        options={availableTags.map((t) => ({
          label: t,
          value: t,
        }))}
        onChange={setValues}
        isMulti
        isClearable
        autoFocus={true}
        placeholder={T.translate("reactSelect.tagPlaceholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
        formatCreateLabel={(inputValue: string) =>
          T.translate("common.create") + `: "${inputValue}"`
        }
      />
      <StyledPrimaryButton type="submit" small disabled={loading}>
        {T.translate("common.save")}
      </StyledPrimaryButton>
    </form>
  );
};

export default EditableTagsForm;
