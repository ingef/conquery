import { ComponentProps } from "react";
import { exists } from "../../../common/helpers/exists";
import { Headline } from "../../config-types";
import {
  getHeadlineFieldAs,
  Headline as HeadlineComponent,
  HeadlineIndex,
} from "../../form-components/Headline";
import Field from "../Field";

export const HeadlineField = ({
  field,
  commonProps: { h1Index, locale },
}: {
  field: Headline;
  commonProps: Omit<ComponentProps<typeof Field>, "field">;
}) => {
  return (
    <HeadlineComponent as={getHeadlineFieldAs(field)} size={field.style?.size}>
      {exists(h1Index) && <HeadlineIndex>{h1Index + 1}</HeadlineIndex>}
      {field.label[locale]}
    </HeadlineComponent>
  );
};
