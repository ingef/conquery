import styled from "@emotion/styled";
import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { DetailedHTMLProps, forwardRef, TextareaHTMLAttributes } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import Labeled from "../Labeled";

const Root = styled("div")`
  position: relative;
`;

const Textarea = styled("textarea")`
  outline: 0;
  width: 100%;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  padding: 6px 30px 6px 10px;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ClearZoneIconButton = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 10px;
  height: 30px;
  cursor: pointer;
  display: flex;
  align-items: center;
`;

interface OtherProps {
  label: string;
  className?: string;
  fullWidth?: boolean;
  indexPrefix?: number;
  tooltip?: string;
  onChange: (value: string | null) => void;
}

type InputTextareaProps = DetailedHTMLProps<
  TextareaHTMLAttributes<HTMLTextAreaElement>,
  HTMLTextAreaElement
>;

export const InputTextarea = forwardRef<
  HTMLTextAreaElement,
  InputTextareaProps & OtherProps
>(({ label, className, indexPrefix, tooltip, onChange, ...props }, ref) => {
  const { t } = useTranslation();

  return (
    <Labeled
      label={label}
      indexPrefix={indexPrefix}
      className={className}
      fullWidth
      tooltip={tooltip}
    >
      <Root>
        <Textarea
          ref={ref}
          {...props}
          onChange={({ target: { value } }) =>
            value.length === 0 ? onChange(null) : onChange(value)
          }
          value={props.value || ""}
        />
        {props.value && (
          <ClearZoneIconButton
            tiny
            icon={faTimes}
            tabIndex={-1}
            title={t("common.clearValue")}
            aria-label={t("common.clearValue")}
            onClick={() => onChange(null)}
          />
        )}
      </Root>
    </Labeled>
  );
});
