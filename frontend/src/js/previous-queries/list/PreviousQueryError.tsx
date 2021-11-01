interface Props {
  query: {
    error: string;
  };
}

const PreviousQueryError = (props: Props) => {
  return (
    !!props.query.error && (
      <div className="previous-query-error">{props.query.error}</div>
    )
  );
};

export default PreviousQueryError;
