interface Props {
  dates: {
    from: string; // 2022-12-31
    to: string; // 2022-12-31
  };
}

const format = (dateStr: string) => {
  const [, month, day] = dateStr.split("-");
  return `${day}.${month}.`;
};

export const RowDates = ({ dates }: Props) => {
  const sameDate = dates.from === dates.to;

  return sameDate ? (
    <code style={{ flexShrink: 0 }}>{format(dates.from)}</code>
  ) : (
    <code style={{ flexShrink: 0 }}>
      {format(dates.from)} {format(dates.to)}
    </code>
  );
};
