import { CheckIcon } from "lucide-react";
import {
  type FocusEvent,
  type FormEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import { TextField as RacTextField } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Button } from "./Button";
import { Input } from "./Input";
import { Tooltip, TooltipTrigger } from "./Tooltip";

const form = tv({
  base: "flex min-w-0 items-center gap-1",
  variants: {
    size: {
      // the input frame has a single 30 px height; shrunk to the small button's 24 px
      sm: "[&_:has(>input)]:h-6",
      md: "",
    },
  },
});

/** Enter or the check saves a changed text; Escape, a click or a tab elsewhere discards it */
const EditableTextForm = ({
  size,
  label,
  text,
  onSubmit,
  onCancel,
}: {
  size: "sm" | "md";
  label?: string;
  text: string;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}) => {
  const { t } = useTranslation();
  const [value, setValue] = useState(text);
  const formRef = useRef<HTMLFormElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
    inputRef.current?.select();
  }, []);

  useClickOutside(formRef, onCancel);

  const onSubmitForm = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const trimmed = value.trim();
    if (trimmed && trimmed !== text) {
      onSubmit(trimmed);
    } else {
      onCancel();
    }
  };

  // no related target: a window switch, or a click, which the click outside handles
  const onBlur = (e: FocusEvent<HTMLFormElement>) => {
    if (e.relatedTarget && !e.currentTarget.contains(e.relatedTarget)) {
      onCancel();
    }
  };

  return (
    <form
      ref={formRef}
      className={form({ size })}
      onSubmit={onSubmitForm}
      onBlur={onBlur}
    >
      <RacTextField
        aria-label={label}
        className="min-w-0"
        value={value}
        onChange={setValue}
      >
        <Input
          ref={inputRef}
          onKeyDown={(e) => {
            if (e.key === "Escape") onCancel();
          }}
        />
      </RacTextField>
      <TooltipTrigger>
        <Button aria-label={t("common.save")} type="submit" size={size}>
          <CheckIcon />
        </Button>
        <Tooltip>{t("common.save")}</Tooltip>
      </TooltipTrigger>
    </form>
  );
};

export default EditableTextForm;
