import React, { PropTypes } from 'react';

const PreviousQueryError = (props) => {
  return !!props.query.error && (
    <div className="previous-query-error">
      {props.query.error}
    </div>
  );
};

PreviousQueryError.propTypes = {
  query: PropTypes.shape({
    error: PropTypes.string,
  }).isRequired
};

export default PreviousQueryError;
