import { ChevronLeftIcon, ChevronRightIcon } from "lucide-react";
import { useState } from "react";
import type { ReactDatePickerCustomHeaderProps } from "react-datepicker";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../../api/types";
import { useMonthName, useMonthNames } from "../../common/helpers/dateHelper";
import { Button } from "../Button";
import { ToggleButton } from "../ToggleButton";
import { textStyle } from "../Typography";

const root = tv({
  base: "flex items-center justify-between",
});

const selectMenuContainer = tv({
  base: ["absolute top-[40px] left-0", "w-full"],
});

const menu = tv({
  base: [
    "w-full",
    "rounded-[4px]",
    "shadow-[0_0_0_1px_hsl(0deg_0%_0%/10%),0_4px_11px_hsl(0deg_0%_0%/10%)]",
    "bg-bg-50",
  ],
});

const optionList = tv({
  base: ["gap-[5px]", "p-[3px]", "overscroll-contain"],
  variants: {
    layout: {
      twoColumns: "grid grid-cols-[auto_auto]",
      oneColumn: ["flex flex-col-reverse", "h-[200px]", "overflow-auto"],
    },
  },
});

const monthYearLabel = tv({
  base: [
    textStyle({ size: 2, strong: true }),
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
      <div className={menu()}>
        <div className={optionList({ layout })}>
          {options.map((option) => (
            <ToggleButton
              intent="secondary"
              size="sm"
              key={option.value}
              isSelected={
                option.value === date.getFullYear() ||
                option.value === date.getMonth()
              }
              onPress={() => onSelect(option.value as number)}
            >
              {option.label}
            </ToggleButton>
          ))}
        </div>
      </div>
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
  const { t } = useTranslation();
  return (
    <div className={root()}>
      <Button
        intent="tertiary"
        aria-label={t("inputDate.previousMonth")}
        onPress={decreaseMonth}
        isDisabled={prevMonthButtonDisabled}
      >
        <ChevronLeftIcon />
      </Button>
      <YearMonthSelect
        date={date}
        changeYear={changeYear}
        changeMonth={changeMonth}
      />
      <Button
        intent="tertiary"
        aria-label={t("inputDate.nextMonth")}
        onPress={increaseMonth}
        isDisabled={nextMonthButtonDisabled}
      >
        <ChevronRightIcon />
      </Button>
    </div>
  );
};
