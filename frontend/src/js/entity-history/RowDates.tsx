import { C3 } from "../ui-components/Typography";

export const formatHistoryDayRange = (dateStr: string) => {
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
    <div className="shrink-0">
      <C3>{formatHistoryDayRange(dates.from)}</C3>
    </div>
  ) : (
    <div className="flex shrink-0 flex-col">
      <C3>{formatHistoryDayRange(dates.from)}</C3>
      <div className="mt-px mb-px ml-[14px] block h-[7px] w-px bg-gray-500" />
      <C3>{formatHistoryDayRange(dates.to)}</C3>
    </div>
  );
};
