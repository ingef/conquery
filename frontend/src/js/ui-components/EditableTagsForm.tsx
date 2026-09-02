import { faCheck, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { type FormEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import WithTooltip from "../tooltip/WithTooltip";

import InputMultiSelect from "./InputMultiSelect/InputMultiSelect";

const form = tv({
  base: "flex items-start",
});

const saveButton = tv({
  base: ["ml-[3px]", "px-[10px] py-[7px]"],
});

const multiSelect = tv({
  base: ["z-2", "grow"],
});

const EditableTagsForm = ({
  className,
  tags,
  loading,
  onSubmit,
  onCancel,
  label,
  availableTags,
}: {
  className?: string;
  tags?: string[];
  loading?: boolean;
  label?: string;
  onSubmit: (tags: string[]) => void;
  onCancel?: () => void;
  availableTags: string[];
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
    <form ref={ref} className={form({ className })} onSubmit={submit}>
      <InputMultiSelect
        className={multiSelect()}
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
        <IconButton
          className={saveButton()}
          type="submit"
          frame
          disabled={!!loading}
          icon={loading ? faSpinner : faCheck}
        />
      </WithTooltip>
    </form>
  );
};

export default EditableTagsForm;
