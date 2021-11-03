import styled from "@emotion/styled";
import { FC } from "react";
import type { WrappedFieldProps } from "redux-form";

import SmallTabNavigation, {
  TabOption,
} from "../../small-tab-navigation/SmallTabNavigation";

interface PropsT extends WrappedFieldProps {
  options: TabOption[];
}

const SxSmallTabNavigation = styled(SmallTabNavigation)`
  padding-top: 3px;
`;

const FormTavNavigation: FC<PropsT> = ({ options, input }) => {
  return (
    <SxSmallTabNavigation
      size="L"
      selectedTab={input.value}
      onSelectTab={input.onChange}
      options={options}
    />
  );
};

export default FormTavNavigation;
