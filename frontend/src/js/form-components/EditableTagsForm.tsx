import styled from "@emotion/styled";
import React, { FC, useState, useRef, FormEvent } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../button/PrimaryButton";
import { useClickOutside } from "../common/helpers/useClickOutside";

import ReactSelect from "./ReactSelect";

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
  const { t } = useTranslation();
  const ref = useRef(null);
  const [values, setValues] = useState<ValueT[]>(
    tags ? tags.map((t) => ({ label: t, value: t })) : [],
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
        placeholder={t("reactSelect.tagPlaceholder")}
        noOptionsMessage={() => t("reactSelect.noResults")}
        formatCreateLabel={(inputValue: string) =>
          t("common.create") + `: "${inputValue}"`
        }
      />
      <StyledPrimaryButton type="submit" small disabled={loading}>
        {t("common.save")}
      </StyledPrimaryButton>
    </form>
  );
};

export default EditableTagsForm;
