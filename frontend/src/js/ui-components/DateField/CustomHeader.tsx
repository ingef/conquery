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
  base: ["absolute inset-x-0 top-[40px] bottom-0", "bg-bg-50"],
});

const optionList = tv({
  base: ["h-full", "gap-[5px]", "p-[3px]", "overflow-auto overscroll-contain"],
  variants: {
    layout: {
      twoColumns: "grid auto-rows-fr grid-cols-2 [&>*]:h-auto",
      oneColumn: "flex flex-col-reverse",
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
  );
};

type Picking = "year" | "month" | null;

const YearMonthSelect = ({
  date,
  changeMonth,
  changeYear,
  picking,
  setPicking,
}: Pick<
  ReactDatePickerCustomHeaderProps,
  "date" | "changeYear" | "changeMonth"
> & {
  picking: Picking;
  setPicking: (picking: Picking) => void;
}) => {
  const yearOptions: SelectOptionT[] = [...Array(100).keys()]
    .map((n) => new Date().getFullYear() - n)
    .map((year) => ({ label: String(year), value: year }));

  const monthNames = useMonthNames();
  const monthOptions: SelectOptionT[] = monthNames.map((month, i) => ({
    label: month,
    value: i,
  }));

  const handleClick = () => setPicking(picking ? null : "year");

  return (
    <>
      {/* biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a button */}
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
      <div className={monthYearLabel()} onClick={handleClick}>
        {useMonthName(date)} {date.getFullYear()}
      </div>
      {picking === "year" && (
        <SelectMenu
          date={date}
          layout="oneColumn"
          options={yearOptions}
          onSelect={(year) => {
            changeYear(year);
            setPicking("month");
          }}
        />
      )}
      {picking === "month" && (
        <SelectMenu
          date={date}
          layout="twoColumns"
          options={monthOptions}
          onSelect={(month) => {
            changeMonth(month);
            setPicking(null);
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
  const [picking, setPicking] = useState<Picking>("year");
  // the month arrows keep their space, so the label stays centered
  const arrow = picking ? "invisible" : undefined;

  return (
    <div className={root()}>
      <span className={arrow}>
        <Button
          intent="tertiary"
          aria-label={t("inputDate.previousMonth")}
          onPress={decreaseMonth}
          isDisabled={prevMonthButtonDisabled}
        >
          <ChevronLeftIcon />
        </Button>
      </span>
      <YearMonthSelect
        date={date}
        changeYear={changeYear}
        changeMonth={changeMonth}
        picking={picking}
        setPicking={setPicking}
      />
      <span className={arrow}>
        <Button
          intent="tertiary"
          aria-label={t("inputDate.nextMonth")}
          onPress={increaseMonth}
          isDisabled={nextMonthButtonDisabled}
        >
          <ChevronRightIcon />
        </Button>
      </span>
    </div>
  );
};
