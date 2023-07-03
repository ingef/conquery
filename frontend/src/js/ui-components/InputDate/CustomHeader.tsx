import styled from "@emotion/styled";
import {
  faChevronLeft,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import { ReactDatePickerCustomHeaderProps } from "react-datepicker";
import { useSelector } from "react-redux";

import { SelectOptionT } from "../../api/types";
import { StateT } from "../../app/reducers";
import IconButton from "../../button/IconButton";
import { TransparentButton } from "../../button/TransparentButton";
import { useMonthName, useMonthNames } from "../../common/helpers/dateHelper";
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
  transition: opacity ${({ theme }) => theme.transitionTime};
  opacity: 0.75;

  &:hover {
    opacity: 1;
  }
`;

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
  const numLastYearsToShow = useSelector<StateT, number>((state) => {
    if (state.startup.config.observationPeriodStart) {
      return (
        new Date().getFullYear() -
        new Date(state.startup.config.observationPeriodStart).getFullYear()
      );
    } else {
      return 10;
    }
  });
  const yearOptions: SelectOptionT[] = [...Array(numLastYearsToShow).keys()]
    .map((n) => new Date().getFullYear() - n)
    .map((year) => ({ label: String(year), value: year }))
    .reverse();

  const monthNames = useMonthNames();
  const monthOptions: SelectOptionT[] = monthNames.map((month, i) => ({
    label: month,
    value: i,
  }));

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
        {useMonthName(date)} {date.getFullYear()}
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
