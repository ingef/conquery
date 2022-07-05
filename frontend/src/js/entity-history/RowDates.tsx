import styled from "@emotion/styled";

const DateText = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
`;

const Line = styled("div")`
  display: block;
  margin: 1px 0 1px 14px;
  width: 1px;
  height: 7px;
  background-color: ${({ theme }) => theme.col.gray};
`;

const format = (dateStr: string) => {
  const [, month, day] = dateStr.split("-");
  return `${day}.${month}.`;
};

interface Props {
  dates: {
    from: string; // 2022-12-31
    to: string; // 2022-12-31
  };
}

export const RowDates = ({ dates }: Props) => {
  const sameDate = dates.from === dates.to;

  return sameDate ? (
    <DateText style={{ flexShrink: 0 }}>{format(dates.from)}</DateText>
  ) : (
    <DateText
      style={{ display: "flex", flexDirection: "column", flexShrink: 0 }}
    >
      {format(dates.from)}
      <Line />
      {format(dates.to)}
    </DateText>
  );
};
