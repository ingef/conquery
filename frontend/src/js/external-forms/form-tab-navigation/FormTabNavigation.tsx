import type { ComponentProps } from "react";

import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

const FormTavNavigation = (
  props: ComponentProps<typeof SmallTabNavigation>,
) => {
  return (
    <SmallTabNavigation
      className="pt-[3px] pl-[10px]"
      size="L"
      variant="primary"
      {...props}
    />
  );
};

export default FormTavNavigation;
