import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { useActiveLang } from "../localization/useActiveLang";
import { ConfirmableTooltip } from "../ui-components/ConfirmableTooltip";
import InputSelect from "../ui-components/InputSelect/InputSelect";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import { setExternalForm } from "./actions";
import type { Form } from "./config-types";
import { selectActiveFormType, selectAvailableForms } from "./stateSelectors";

const root = tv({
  base: [
    "relative",
    "z-2",
    "shrink-0",
    "box-border",
    "pt-2 pr-5 pb-[10px] pl-[10px]",
    "bg-bg-50",
    "shadow-[0_0_3px_0_rgba(0,0,0,0.3)]",
  ],
});

const FormsNavigation = ({ onReset }: { onReset: () => void }) => {
  const language = useActiveLang();
  const { t } = useTranslation();

  const availableForms = useSelector<
    StateT,
    {
      [formName: string]: Form;
    }
  >((state) => selectAvailableForms(state));

  const activeForm = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const dispatch = useDispatch();

  const onChangeToForm = (form: string) => {
    dispatch(setExternalForm({ form }));
  };

  const options = Object.values(availableForms)
    .map((formType) => ({
      label: formType.title[language]!,
      value: formType.type,
    }))
    .sort((a, b) => (a.label < b.label ? -1 : 1));

  return (
    <div className={root()}>
      <div className="flex flex-row items-end">
        <InputSelect
          className="grow"
          dataTestId="form-select"
          label={t("externalForms.forms")}
          options={options}
          value={options.find((o) => o.value === activeForm) || null}
          onChange={(value) => {
            if (value) {
              onChangeToForm(value.value as string);
              // we intentionally only change the form
              // but we don't reset field state,
              // so values are kept when switching forms
            }
          }}
        />
        <TooltipTrigger>
          <ConfirmableTooltip
            onConfirm={onReset}
            confirmationText={t("externalForms.common.clearConfirm")}
          >
            <IconButton
              className="ml-[10px] shrink-0 px-[10px] py-[7px]"
              frame
              icon={faTrash}
            />
          </ConfirmableTooltip>
          <Tooltip>{t("externalForms.common.clear")}</Tooltip>
        </TooltipTrigger>
      </div>
    </div>
  );
};

export default FormsNavigation;
