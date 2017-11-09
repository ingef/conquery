import React, { PropTypes }    from 'react';
import T                       from 'i18n-react';
import classnames              from 'classnames';
import clickOutside            from 'react-onclickoutside';

// A multi-select where new items can be created
import { Creatable as Select } from 'react-select';

class EditableTagsForm extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      values: props.tags || []
    };
  }

  componentDidMount() {
    this.refs.input.select.focus();
  }

  handleClickOutside() {
    this.props.onCancel();
  }

  _onSelect(inputValues) {
    const values = inputValues.map(v => v.value);

    this.setState({ values })
  }

  _onSubmit(e) {
    e.preventDefault();

    this.props.onSubmit(this.state.values);
  }

  render() {
    return (
      <form
        className={classnames(
          'editable-tags-form',
          this.props.className,
        )}
        onSubmit={this._onSubmit.bind(this)}
      >
        <Select
          ref="input"
          name="input"
          value={this.state.values.map(t => ({ label: t, value: t}))}
          options={this.props.availableTags.map(t => ({ label: t, value: t}))}
          onChange={this._onSelect.bind(this)}
          multi
          promptTextCreator={(label) => T.translate('reactSelect.createTag', {label})}
          placeholder={T.translate('reactSelect.tagPlaceholder')}
          backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
          clearAllText={T.translate('reactSelect.clearAll')}
          clearValueText={T.translate('reactSelect.clearValue')}
          noResultsText={T.translate('reactSelect.noResults')}
        />
        <button
          type="submit"
          className="editable-tags-form__btn btn btn--small btn--primary"
          disabled={this.props.loading}
        >
          { T.translate('common.save') }
        </button>
      </form>
    );
  }
};

EditableTagsForm.propTypes = {
  className: PropTypes.string,
  tags: PropTypes.arrayOf(PropTypes.string),
  loading: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
  availableTags: PropTypes.arrayOf(PropTypes.string),
};

export default clickOutside(EditableTagsForm);
