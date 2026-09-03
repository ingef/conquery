import {
  faChevronLeft,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";
import { useState } from "react";
import type { ReactDatePickerCustomHeaderProps } from "react-datepicker";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";
import { useMonthName, useMonthNames } from "../../common/helpers/dateHelper";
import { Button } from "../Button";
import { List, Menu } from "../InputSelect/InputSelectComponents";

const root = tv({
  base: "flex items-center justify-between",
});

const selectMenuContainer = tv({
  base: ["absolute top-[40px] left-0", "w-full"],
});

const optionList = tv({
  base: "gap-[5px]",
  variants: {
    layout: {
      twoColumns: "grid grid-cols-[auto_auto]",
      oneColumn: ["flex flex-col-reverse", "h-[200px]", "overflow-auto"],
    },
  },
});

const monthYearLabel = tv({
  base: [
    "font-bold",
    "cursor-pointer",
    "transition-opacity duration-100",
    "opacity-75 hover:opacity-100",
  ],
});

const SelectMenu = ({
  date,
  layout,
  options,
  onSelect,
}: Pick<ReactDatePickerCustomHeaderProps, "date"> & {
  options: SelectOptionT[];
  layout: "oneColumn" | "twoColumns";
  onSelect: (n: number) => void;
}) => {
  return (
    <div className={selectMenuContainer()}>
      <Menu>
        <List className={optionList({ layout })}>
          {options.map((option) => (
            <Button
              intent="secondary"
              size="sm"
              key={option.value}
              aria-pressed={
                option.value === date.getFullYear() ||
                option.value === date.getMonth()
              }
              onPress={() => onSelect(option.value as number)}
            >
              {option.label}
            </Button>
          ))}
        </List>
      </Menu>
    </div>
  );
};

const YearMonthSelect = ({
  date,
  changeMonth,
  changeYear,
}: Pick<
  ReactDatePickerCustomHeaderProps,
  "date" | "changeYear" | "changeMonth"
>) => {
  const yearOptions: SelectOptionT[] = [...Array(100).keys()]
    .map((n) => new Date().getFullYear() - n)
    .map((year) => ({ label: String(year), value: year }));

  const monthNames = useMonthNames();
  const monthOptions: SelectOptionT[] = monthNames.map((month, i) => ({
    label: month,
    value: i,
  }));

  const [yearSelectOpen, setYearSelectOpen] = useState(true);
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
      {/* biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a button */}
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
      <div className={monthYearLabel()} onClick={handleClick}>
        {useMonthName(date)} {date.getFullYear()}
      </div>
      {yearSelectOpen && (
        <SelectMenu
          date={date}
          layout="oneColumn"
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
          layout="twoColumns"
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
    <div className={root()}>
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
    </div>
  );
};
