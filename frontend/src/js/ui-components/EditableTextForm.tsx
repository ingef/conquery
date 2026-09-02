import { faCheck, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { type FormEvent, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import WithTooltip from "../tooltip/WithTooltip";

const input = tv({
  base: ["h-[28px]", "px-2", "rounded", "border border-gray-500", "text-sm"],
});

const form = tv({
  base: "flex items-center",
});

const saveButton = tv({
  base: ["ml-[3px]", "px-[10px] py-[6px]"],
});

const EditableTextForm = ({
  className,
  text,
  loading,
  selectTextOnMount,
  saveOnClickoutside,
  onSubmit,
  onCancel,
}: {
  className?: string;
  text: string;
  loading?: boolean;
  selectTextOnMount?: boolean;
  saveOnClickoutside?: boolean;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}) => {
  const { t } = useTranslation();
  const [value, setValue] = useState<string>(text);
  const [textSelected, setTextSelected] = useState<boolean>(false);
  const ref = useRef(null);

  function onSubmitForm(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();

    onSubmit(value);
  }

  useClickOutside(ref, saveOnClickoutside ? () => onSubmit(value) : onCancel);

  return (
    <form ref={ref} className={form({ className })} onSubmit={onSubmitForm}>
      <input
        className={input()}
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        ref={(instance: HTMLInputElement) => {
          if (instance) {
            instance.focus();
            if (selectTextOnMount && !textSelected) {
              instance.select();
              setTextSelected(true);
            }
          }
        }}
      />
      {!saveOnClickoutside && (
        <WithTooltip text={t("common.save")}>
          <IconButton
            className={saveButton()}
            type="submit"
            frame
            disabled={loading}
            icon={loading ? faSpinner : faCheck}
          />
        </WithTooltip>
      )}
    </form>
  );
};

export default EditableTextForm;
