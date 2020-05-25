import React, { FC } from "react";
import styled from "@emotion/styled";

import type { FieldPropsType } from "redux-form";
import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

interface OptionsT {
  label: string;
  value: string;
}

interface PropsT extends FieldPropsType {
  options: OptionsT[];
}

const SxSmallTabNavigation = styled(SmallTabNavigation)`
  padding-top: 3px;
`;

const FormTavNavigation: FC<PropsT> = ({ options, input }) => {
  return (
    <SxSmallTabNavigation
      selectedTab={input.value}
      onSelectTab={input.onChange}
      options={options}
    />
  );
};

export default FormTavNavigation;
