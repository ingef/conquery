import { CheckIcon, LoaderCircleIcon } from "lucide-react";
import { type FormEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../api/types";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Button } from "./Button";
import { ComboBoxMultiField } from "./ComboBoxMultiField";
import { Tooltip, TooltipTrigger } from "./Tooltip";

const form = tv({
  base: "flex items-start",
});

const EditableTagsForm = ({
  className,
  tags,
  loading,
  onSubmit,
  onCancel,
  availableTags,
}: {
  className?: string;
  tags?: string[];
  loading?: boolean;
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
      <div className="grow">
        <ComboBoxMultiField
          aria-label={t("inputMultiSelect.folders")}
          creatable
          autoFocus
          value={values}
          options={availableTags.map((t) => ({
            label: t,
            value: t,
          }))}
          onChange={setValues}
          placeholder={t("inputMultiSelect.tagPlaceholder")}
        />
      </div>
      <div className="ml-[3px]">
        <TooltipTrigger>
          <Button
            aria-label={t("common.save")}
            intent="secondary"
            type="submit"
            isDisabled={!!loading}
          >
            {loading ? <LoaderCircleIcon /> : <CheckIcon />}
          </Button>
          <Tooltip>{t("common.save")}</Tooltip>
        </TooltipTrigger>
      </div>
    </form>
  );
};

export default EditableTagsForm;
