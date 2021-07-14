import styled from "@emotion/styled";
import React, { FC, useState, useRef, FormEvent } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import WithTooltip from "../tooltip/WithTooltip";

import ReactSelect from "./ReactSelect";

interface PropsT {
  className?: string;
  tags?: string[];
  loading?: boolean;
  onSubmit: (tags: string[]) => void;
  onCancel: () => void;
  availableTags: string[];
}

interface ValueT {
  label: string;
  value: string;
}

const Form = styled("form")`
  display: flex;
  align-items: flex-start;
`;

const SxIconButton = styled(IconButton)`
  padding: 10px 10px;
  margin-left: 3px;
`;

const SxReactSelect = styled(ReactSelect)`
  z-index: 2;
  flex-grow: 1;
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
    <Form ref={ref} className={className} onSubmit={submit}>
      <SxReactSelect
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
      <WithTooltip text={t("common.save")}>
        <SxIconButton
          type="submit"
          frame
          disabled={!!loading}
          icon={loading ? "spinner" : "check"}
        />
      </WithTooltip>
    </Form>
  );
};

export default EditableTagsForm;
