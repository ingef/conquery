import styled from "@emotion/styled";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import { useActiveLang } from "../localization/useActiveLang";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";
import InputSelect from "../ui-components/InputSelect/InputSelect";

import { setExternalForm } from "./actions";
import type { Form } from "./config-types";
import { selectActiveFormType, selectAvailableForms } from "./stateSelectors";

const Root = styled("div")`
  flex-shrink: 0;
  padding: 8px 20px 10px 10px;
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
  background-color: ${({ theme }) => theme.col.bg};
  z-index: 1;
`;

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: flex-end;
`;

const SxInputSelect = styled(InputSelect)`
  flex-grow: 1;
`;

const SxIconButton = styled(IconButton)`
  flex-shrink: 0;
  margin-left: 10px;
  padding: 7px 10px;
`;

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
    <Root>
      <Row>
        <SxInputSelect
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
        <ConfirmableTooltip
          onConfirm={onReset}
          confirmationText={t("externalForms.common.clearConfirm")}
        >
          <WithTooltip text={t("externalForms.common.clear")}>
            <SxIconButton frame icon={faTrash} />
          </WithTooltip>
        </ConfirmableTooltip>
      </Row>
    </Root>
  );
};

export default FormsNavigation;
