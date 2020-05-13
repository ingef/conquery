import React, { useState } from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import clickOutside from "react-onclickoutside";

import PrimaryButton from "../button/PrimaryButton";

interface PropsT {
  className?: string;
  text: string;
  loading: boolean;
  selectTextOnMount?: boolean;
  onSubmit: (text: string) => void;
  onCancel: () => void;
}

const Input = styled("input")`
  height: 30px;
  font-size: ${({ theme }) => theme.font.sm};
  margin-right: 3px;
  margin-bottom: 3px;
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
  onSubmit,
  onCancel
}) => {
  EditableTextForm.handleClickOutside = onCancel;

  const [value, setValue] = useState<string>(text);
  const [textSelected, setTextSelected] = useState<boolean>(false);

  function onSubmitForm(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();

    onSubmit(value);
  }

  return (
    <Form className={className} onSubmit={onSubmitForm}>
      <Input
        type="text"
        value={value}
        onChange={e => setValue(e.target.value)}
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
      <SxPrimaryButton type="submit" small disabled={loading}>
        {T.translate("common.save")}
      </SxPrimaryButton>
    </Form>
  );
};

// handleClickOutside() {
//   this.props.onCancel();
// }

export default clickOutside(EditableTextForm, {
  handleClickOutside: () => EditableTextForm.handleClickOutside
});
