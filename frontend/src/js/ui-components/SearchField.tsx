import { SearchIcon, XIcon } from "lucide-react";
import {
  ButtonContext,
  SearchField as RacSearchField,
  type SearchFieldProps as RacSearchFieldProps,
} from "react-aria-components";

import { Icon } from "./Icon";
import { Input, InputButton } from "./Input";
import { type FieldLabelProps, Label } from "./Label";

export type SearchFieldProps = Omit<
  RacSearchFieldProps,
  | "className"
  | "style"
  | "children"
  | "isInvalid"
  | "validate"
  | "validationBehavior"
  | "aria-label"
> &
  FieldLabelProps & {
    placeholder?: string;
  };

/** A search field on react-aria's SearchField: `onSubmit` on Enter and the search button, `onClear` on Escape and the clear button. */
export const SearchField = ({
  label,
  placeholder,
  onSubmit,
  ...props
}: SearchFieldProps) => (
  <RacSearchField className="min-w-0" onSubmit={onSubmit} {...props}>
    {({ isDisabled, isEmpty, state }) => (
      <>
        {label && <Label>{label}</Label>}
        <Input
          placeholder={placeholder}
          isDisabled={isDisabled}
          addonRight={
            !isEmpty && (
              <>
                {/* SearchField hands its clear behavior to every Button inside; this one submits */}
                <ButtonContext.Provider value={null}>
                  <InputButton
                    aria-label={placeholder ?? label}
                    isDisabled={isDisabled}
                    onPress={() => onSubmit?.(state.value)}
                  >
                    <Icon icon={SearchIcon} />
                  </InputButton>
                </ButtonContext.Provider>
                <InputButton>
                  <Icon icon={XIcon} />
                </InputButton>
              </>
            )
          }
        />
      </>
    )}
  </RacSearchField>
);
