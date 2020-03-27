import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import clickOutside from "react-onclickoutside";

import PrimaryButton from "../button/PrimaryButton";

interface PropsType {
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

class EditableTextForm extends React.Component {
  props: PropsType;

  componentDidMount() {
    this.refs.input.focus();
    this.refs.input.value = this.props.text;

    if (this.props.selectTextOnMount) this.refs.input.select();
  }

  handleClickOutside() {
    this.props.onCancel();
  }

  _onSubmit(e) {
    e.preventDefault();

    const { value } = this.refs.input;

    this.props.onSubmit(value);
  }

  render() {
    return (
      <Form
        className={this.props.className}
        onSubmit={this._onSubmit.bind(this)}
      >
        <Input type="text" ref="input" placeholder={this.props.text} />
        <SxPrimaryButton type="submit" small disabled={this.props.loading}>
          {T.translate("common.save")}
        </SxPrimaryButton>
      </Form>
    );
  }
}

export default clickOutside(EditableTextForm);
