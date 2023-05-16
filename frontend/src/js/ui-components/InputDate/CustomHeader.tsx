import styled from "@emotion/styled";
import {
  faChevronLeft,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { ReactDatePickerCustomHeaderProps } from "react-datepicker";

import { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { TransparentButton } from "../../button/TransparentButton";
import { getMonthName, getMonthNames } from "../../common/helpers/dateHelper";
import { List, Menu } from "../InputSelect/InputSelectComponents";

export const Root = styled("div")`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const SelectMenuContainer = styled("div")`
  position: absolute;
  top: 40px;
  left: 0;
  width: 100%;
`;

export const OptionList = styled(List)`
  display: grid;
  grid-template-columns: auto auto;
  gap: 5px;
`;

export const MonthYearLabel = styled("div")`
  font-weight: bold;
  cursor: pointer;
`;

const yearSelectionSpan = 10;
const yearOptions: SelectOptionT[] = [...Array(yearSelectionSpan).keys()]
  .map((n) => new Date().getFullYear() - n)
  .map((year) => ({
    label: String(year),
    value: year,
  }))
  .reverse();

const monthOptions: SelectOptionT[] = getMonthNames().map((month, i) => ({
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
          <TransparentButton
            small
            key={option.value}
            active={
              option.value === date.getFullYear() ||
              option.value === date.getMonth()
            }
            onClick={() => onSelect(option.value as number)}
          >
            {option.label}
          </TransparentButton>
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
        {getMonthName(date)} {date.getFullYear()}
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
