import styled from "@emotion/styled";
import { FC, FormEvent, useState, useRef } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { useClickOutside } from "../common/helpers/useClickOutside";
import WithTooltip from "../tooltip/WithTooltip";

interface PropsT {
  className?: string;
  text: string;
  loading?: boolean;
  selectTextOnMount?: boolean;
  saveOnClickoutside?: boolean;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}

const Input = styled("input")`
  font-size: ${({ theme }) => theme.font.sm};
  padding: 4px 8px;
`;

const Form = styled("form")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
`;

const SxIconButton = styled(IconButton)`
  padding: 4px 10px;
  margin-left: 3px;
`;

const EditableTextForm: FC<PropsT> = ({
  className,
  text,
  loading,
  selectTextOnMount,
  saveOnClickoutside,
  onSubmit,
  onCancel,
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
    <Form ref={ref} className={className} onSubmit={onSubmitForm}>
      <Input
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
          <SxIconButton
            type="submit"
            frame
            disabled={loading}
            icon={loading ? "spinner" : "check"}
          />
        </WithTooltip>
      )}
    </Form>
  );
};

export default EditableTextForm;
