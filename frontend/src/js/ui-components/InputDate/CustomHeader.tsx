import {
  faChevronLeft,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { ReactDatePickerCustomHeaderProps } from "react-datepicker";

import { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { Menu } from "../InputSelect/InputSelectComponents";

import {
  MonthYearLabel,
  OptionButton,
  OptionList,
  Root,
  SelectMenuContainer,
} from "./CustomHeaderComponents";

const yearOptions: SelectOptionT[] = [...Array(10).keys()]
  .map((n) => new Date().getFullYear() - n)
  .map((year) => ({
    label: String(year),
    value: year,
  }))
  .reverse();

const months = [
  "Januar",
  "Februar",
  "März",
  "April",
  "Mai",
  "Juni",
  "Juli",
  "August",
  "September",
  "Oktober",
  "November",
  "Dezember",
];
const monthOptions: SelectOptionT[] = months.map((month, i) => ({
  label: month,
  value: i,
}));

const SelectMenu = ({
  date,
  options,
  onSelect,
}: Pick<ReactDatePickerCustomHeaderProps, "date"> & {
  options: SelectOptionT[];
  onSelect: (n: number) => void;
}) => (
  <SelectMenuContainer>
    <Menu>
      <OptionList>
        {options.map((option) => (
          <OptionButton
            small
            key={option.value}
            active={option.value === date.getFullYear()}
            onClick={() => onSelect(option.value as number)}
          >
            {option.label}
          </OptionButton>
        ))}
      </OptionList>
    </Menu>
  </SelectMenuContainer>
);

const YearMonthSelect = ({
  date,
  changeMonth,
  changeYear,
}: Pick<
  ReactDatePickerCustomHeaderProps,
  "date" | "changeYear" | "changeMonth"
>) => {
  const [yearSelectOpen, setYearSelectOpen] = useState(false);
  const [monthSelectOpen, setMonthSelectOpen] = useState(false);
  const handleClick = () => {
    if (yearSelectOpen || monthSelectOpen) {
      setYearSelectOpen(false);
      setMonthSelectOpen(false);
    } else {
      setYearSelectOpen(true);
    }
  };

  return (
    <>
      <MonthYearLabel onClick={handleClick}>
        {months[date.getMonth()]} {date.getFullYear()}
      </MonthYearLabel>
      {yearSelectOpen && (
        <SelectMenu
          date={date}
          options={yearOptions}
          onSelect={(year) => {
            changeYear(year);
            setYearSelectOpen(false);
            setMonthSelectOpen(true);
          }}
        />
      )}
      {monthSelectOpen && (
        <SelectMenu
          date={date}
          options={monthOptions}
          onSelect={(month) => {
            changeMonth(month);
            setMonthSelectOpen(false);
          }}
        />
      )}
    </>
  );
};

export const CustomHeader = ({
  date,
  changeYear,
  changeMonth,
  decreaseMonth,
  increaseMonth,
  prevMonthButtonDisabled,
  nextMonthButtonDisabled,
}: ReactDatePickerCustomHeaderProps) => {
  return (
    <Root>
      <IconButton
        icon={faChevronLeft}
        onClick={decreaseMonth}
        disabled={prevMonthButtonDisabled}
      />
      <YearMonthSelect
        date={date}
        changeYear={changeYear}
        changeMonth={changeMonth}
      />
      <IconButton
        icon={faChevronRight}
        onClick={increaseMonth}
        disabled={nextMonthButtonDisabled}
      />
    </Root>
  );
};
