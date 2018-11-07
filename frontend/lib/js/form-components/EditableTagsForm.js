import React            from 'react';
import PropTypes        from 'prop-types';
import T                from 'i18n-react';
import classnames       from 'classnames';
import clickOutside     from 'react-onclickoutside';

// A multi-select where new items can be created
import { Creatable as Select}  from 'react-select';

class EditableTagsForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      values: (props.tags && props.tags.map(t => ({ label: t, value: t}))) || []
    };
  }

  handleClickOutside() {
    this.props.onCancel();
  }

  handleChange = (values: any, actionMeta: any) => {
    this.setState({ values });
  };

  _onSubmit(e) {
    e.preventDefault();

    this.props.onSubmit(this.state.values.map(v => v.value));
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
          name="input"
          value={this.state.values}
          options={this.props.availableTags.map(t => ({ label: t, value: t}))}
          onChange={this.handleChange}
          isMulti
          isClearable
          autoFocus={true}
          placeholder={T.translate('reactSelect.tagPlaceholder')}
          noOptionsMessage={() => T.translate('reactSelect.noResults')}
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
