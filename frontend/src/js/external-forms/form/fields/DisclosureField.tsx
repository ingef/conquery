import styled from "@emotion/styled";
import {
  faChevronDown,
  faChevronRight,
} from "@fortawesome/free-solid-svg-icons";
import { ComponentProps, useState } from "react";
import { exists } from "../../../common/helpers/exists";
import FaIcon from "../../../icon/FaIcon";
import InfoTooltip from "../../../tooltip/InfoTooltip";
import { Disclosure } from "../../config-types";
import { getFieldKey } from "../../helper";
import Field from "../Field";

const Details = styled("details")`
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.col.gray};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Summary = styled("summary")`
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 12px 12px 12px;
  background-color: white;
  font-size: ${({ theme }) => theme.font.sm};
  font-weight: 400;
`;

const DisclosureNestedFields = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 7px;
  background-color: ${({ theme }) => theme.col.bg};
  border-top: 1px solid ${({ theme }) => theme.col.gray};
  padding: 12px 10px 12px;
`;

export const DisclosureField = ({
  field,
  commonProps,
}: {
  field: Disclosure;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  const [isOpen, setOpen] = useState(false);

  if (field.fields.length === 0) return null;

  const { formType, locale } = commonProps;

  return (
    <Details open={isOpen} onToggle={() => setOpen(!isOpen)}>
      <Summary>
        <span style={{ width: "20px" }}>
          <FaIcon icon={isOpen ? faChevronDown : faChevronRight} />
        </span>
        {field.label[locale]}
        {exists(field.tooltip) && <InfoTooltip text={field.tooltip[locale]} />}
      </Summary>
      <DisclosureNestedFields>
        {field.fields.map((f, i) => {
          const key = getFieldKey(formType, f, i);

          return <Field key={key} field={f} {...commonProps} />;
        })}
      </DisclosureNestedFields>
    </Details>
  );
};
