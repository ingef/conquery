// @flow

import React                from 'react';
import T                    from 'i18n-react';
import classnames           from 'classnames';
import clickOutside         from 'react-onclickoutside';



type PropsType = {
  className?: string,
  text: string,
  loading: boolean,
  selectTextOnMount: boolean,
  onSubmit: Function,
  onCancel: Function,
};

class EditableTextForm extends React.Component {
  props: PropsType;

  componentDidMount() {
    this.refs.input.focus();
    this.refs.input.value = this.props.text;
    if (this.props.selectTextOnMount)
      this.refs.input.select();
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
      <form
        className={classnames(
          'editable-text-form',
          this.props.className,
        )}
        onSubmit={this._onSubmit.bind(this)}
      >
        <input
          className="editable-text-form__input"
          type="text"
          ref="input"
          placeholder={this.props.text}
        />
        <button
          type="submit"
          className="btn btn--small btn--primary"
          disabled={this.props.loading}
        >
          { T.translate('common.save') }
        </button>
      </form>
    );
  }
};


export default clickOutside(EditableTextForm);
