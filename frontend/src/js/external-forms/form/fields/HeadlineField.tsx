import type { ComponentProps } from "react";
import { exists } from "../../../common/helpers/exists";
import type { Headline } from "../../config-types";
import { Headline as HeadlineComponent } from "../../form-components/Headline";
import type Field from "../Field";

export const HeadlineField = ({
  field,
  commonProps: { h1Index, locale },
}: {
  field: Headline;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => (
  <HeadlineComponent
    size={field.style?.size}
    index={exists(h1Index) ? h1Index + 1 : undefined}
  >
    {field.label[locale]}
  </HeadlineComponent>
);
