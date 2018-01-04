import React                           from 'react';
import PropTypes                       from 'prop-types';
import T                               from 'i18n-react';
import { Creatable as Select }         from 'react-select';

const SearchBox = (props) => {
  return (
    <div className="search-box">
      <Select
        name="input"
        value={props.search.map(t => ({ label: t, value: t }))}
        options={
          props.options
          ? props.options.map(t => ({ label: t, value: t}))
          : []
        }
        onChange={(values) => props.onSearch(values.map(v => v.value))}
        multi
        promptTextCreator={(label) => T.translate(
          'reactSelect.searchFor',
          { label }
        )}
        placeholder={T.translate('reactSelect.searchPlaceholder')}
        backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
        clearAllText={T.translate('reactSelect.clearAll')}
        clearValueText={T.translate('reactSelect.clearValue')}
        noResultsText={T.translate('reactSelect.noResults')}
      />
    </div>
  );
};

SearchBox.propTypes = {
  search: PropTypes.arrayOf(PropTypes.string).isRequired,
  onSearch: PropTypes.func.isRequired,
  options: PropTypes.arrayOf(PropTypes.string),
};

export default SearchBox;
