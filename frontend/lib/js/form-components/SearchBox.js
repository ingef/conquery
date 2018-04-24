import React                    from 'react';
import { Dot }                  from 'react-animated-dots';
import PropTypes                from 'prop-types';
import T                        from 'i18n-react';
import { Creatable as Select }  from 'react-select';
import { DelayInput }           from 'react-delay-input';
import { isEmpty, duration }    from '../common/helpers';

const SearchBox = (props) => {
  const { searchResult } = props;

  return props.isMulti
    ? <div className="search-box">
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
    : <div className="search-box input--full-width">
        <DelayInput
          delayTimeout={500}
          className="search-box__input"
          placeholder={T.translate('search.placeholder')}
          value={searchResult.query || ""}
          onChange={e => props.onSearch(props.datasetId, e.target.value)}
        />
        {
          searchResult.loading
          ? <span className="dots"><Dot>.</Dot><Dot>.</Dot><Dot>.</Dot></span>
          : searchResult.searching && searchResult.resultCount >= 0 &&
            <span className="input input-label--disabled input-label--tiny">
              {
                T.translate('search.resultLabel', {
                  limit: searchResult.limit,
                  resultCount: searchResult.resultCount,
                  duration: duration(
                      searchResult.duration,
                      "milliseconds",
                      T.translate("search.durationFormat")
                    )
                })
              }
            </span>
        }
        {
          !isEmpty(searchResult.query) &&
          <span
            className="search-box__clear-zone"
            title={T.translate('common.clearValue')}
            aria-label={T.translate('common.clearValue')}
            onClick={() => props.onSearch('')}
          >
            ×
          </span>
        }
      </div>
};

SearchBox.propTypes = {
  search: PropTypes.arrayOf(PropTypes.string),
  onSearch: PropTypes.func.isRequired,
  options: PropTypes.arrayOf(PropTypes.string),
  isMulti: PropTypes.object,
  searchResult: PropTypes.object,
  datasetId: PropTypes.string
};

export default SearchBox;
