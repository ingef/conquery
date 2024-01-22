import styled from "@emotion/styled";
import { faCheck, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { FC, FormEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import WithTooltip from "../tooltip/WithTooltip";

import InputMultiSelect from "./InputMultiSelect/InputMultiSelect";

interface PropsT {
  className?: string;
  tags?: string[];
  loading?: boolean;
  label?: string;
  onSubmit: (tags: string[]) => void;
  onCancel?: () => void;
  availableTags: string[];
}

const Form = styled("form")`
  display: flex;
  align-items: flex-start;
`;

const SxIconButton = styled(IconButton)`
  padding: 7px 10px;
  margin-left: 3px;
`;

const SxInputMultiSelect = styled(InputMultiSelect)`
  z-index: 2;
  flex-grow: 1;
`;

const EditableTagsForm: FC<PropsT> = ({
  className,
  tags,
  loading,
  onSubmit,
  onCancel,
  label,
  availableTags,
}) => {
  const { t } = useTranslation();
  const ref = useRef(null);
  const [values, setValues] = useState<SelectOptionT[]>(
    tags ? tags.map((t) => ({ label: t, value: t })) : [],
  );
  useClickOutside(ref, () => {
    if (onCancel) {
      onCancel();
    }
  });

  function submit(e: FormEvent) {
    e.preventDefault();

    onSubmit(values ? values.map((v) => v.value as string) : []);
  }

  return (
    <Form ref={ref} className={className} onSubmit={submit}>
      <SxInputMultiSelect
        creatable
        autoFocus
        label={label}
        value={values}
        options={availableTags.map((t) => ({
          label: t,
          value: t,
        }))}
        onChange={setValues}
        placeholder={t("inputMultiSelect.tagPlaceholder")}
      />
      <WithTooltip text={t("common.save")}>
        <SxIconButton
          type="submit"
          frame
          disabled={!!loading}
          icon={loading ? faSpinner : faCheck}
        />
      </WithTooltip>
    </Form>
  );
};

export default EditableTagsForm;
