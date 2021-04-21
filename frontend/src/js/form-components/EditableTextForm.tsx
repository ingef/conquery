import styled from "@emotion/styled";
import React, { useState, useRef } from "react";
import { useTranslation } from "react-i18next";

import PrimaryButton from "../button/PrimaryButton";
import { useClickOutside } from "../common/helpers/useClickOutside";

interface PropsT {
  className?: string;
  text: string;
  loading: boolean;
  selectTextOnMount?: boolean;
  saveOnClickoutside?: boolean;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}

const Input = styled("input")`
  height: 30px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const Form = styled("form")`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
`;

const SxPrimaryButton = styled(PrimaryButton)`
  margin-bottom: 3px;
`;

const EditableTextForm: React.FC<PropsT> = ({
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

  function onSubmitForm(e: React.FormEvent<HTMLFormElement>) {
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
        <SxPrimaryButton type="submit" small disabled={loading}>
          {t("common.save")}
        </SxPrimaryButton>
      )}
    </Form>
  );
};

export default EditableTextForm;
