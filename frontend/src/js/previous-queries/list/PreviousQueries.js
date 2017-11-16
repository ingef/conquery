import React, { PropTypes, Component } from 'react';
import T                               from 'i18n-react';
import ReactList                       from 'react-list';

import { ErrorMessage }                from '../../error-message';
import PreviousQuery                   from './PreviousQuery';


class PreviousQueries extends Component {
  static propTypes = {
    datasetId: PropTypes.string,
    queries: PropTypes.array.isRequired,
    loading: PropTypes.bool,
    error: PropTypes.string,
    loadQueries: PropTypes.func.isRequired,
  }

  componentDidMount() {
    this.props.loadQueries();
  }

  _renderQuery = (index, key) => {
    return (
      <div key={key} className="previous-query-container">
        <PreviousQuery query={this.props.queries[index]} datasetId={this.props.datasetId} />
      </div>
    );
  }

  render() {
    const { loading, error } = this.props;

    return (
      <div className="previous-queries">
        {
          error &&
          <ErrorMessage
            className="previous-queries__error"
            message={T.translate('previousQueries.error')}
          />
        }
        {
          loading &&
          <p className="previous-queries__loading">
            <span className="previous-queries__spinner">
              <i className="fa fa-spinner" />
            </span>
            <span>
              { T.translate('previousQueries.loading') }
            </span>
          </p>
        }
        {
          this.props.queries.length === 0 && !loading && !error &&
          T.translate('previousQueries.noQueriesFound')
        }
        {
          <ReactList
            itemRenderer={this._renderQuery}
            length={this.props.queries.length}
            type="variable"
          />
        }
      </div>
    );
  }
};

export default PreviousQueries;
