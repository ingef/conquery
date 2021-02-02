import React, { useState, useRef } from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/react";

import T from "i18n-react";
import { useDrag } from "react-dnd";
import { useSelector, useDispatch } from "react-redux";
import { parseISO } from "date-fns";

import type { StateT } from "app-types";
import type { DatasetIdT } from "../../api/types";

import { FORM_CONFIG } from "../../common/constants/dndTypes";
import SelectableLabel from "../../highlightable-label/HighlightableLabel";

import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import EditableText from "../../form-components/EditableText";
import EditableTags from "../../form-components/EditableTags";

import { formatDateDistance } from "../../common/helpers";
import { FormConfigT } from "./reducer";
import { usePatchFormConfig } from "../../api/api";
import FormConfigTags from "./FormConfigTags";
import { patchFormConfigSuccess } from "./actions";
import { setMessage } from "../../snack-message/actions";
import { useIsLabelHighlighted } from "./selectors";
import { useFormLabelByType } from "../stateSelectors";
import { getWidthAndHeight } from "../../app/DndProvider";

const Root = styled("div")<{ own: boolean; system: boolean; shared: boolean }>`
  margin: 0;
  padding: 5px 10px;
  cursor: pointer;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  background-color: ${({ theme }) => theme.col.bg};
  box-shadow: 0 0 2px 0 rgba(0, 0, 0, 0.2);

  border-left: ${({ theme, own, system }) =>
    own
      ? `4px solid ${theme.col.orange}`
      : system
      ? `4px solid ${theme.col.blueGrayDark}`
      : `1px solid ${theme.col.grayLight}`};

  &:hover {
    ${({ theme, own, system }) =>
      !own &&
      !system &&
      css`
        border-left-color: ${theme.col.blueGray};
      `};
    border-top-color: ${({ theme }) => theme.col.blueGray};
    border-right-color: ${({ theme }) => theme.col.blueGray};
    border-bottom-color: ${({ theme }) => theme.col.blueGray};
  }
`;

const Gray = styled("div")`
  color: ${({ theme }) => theme.col.gray};
`;
const TopInfos = styled(Gray)`
  line-height: 24px;
`;

const MiddleRight = styled(Gray)`
  text-align: right;
`;

const TopRight = styled("div")`
  float: right;
`;
const SharedIndicator = styled("span")`
  margin-left: 10px;
  color: ${({ theme }) => theme.col.blueGray};
`;
const StyledSelectableLabel = styled(SelectableLabel)`
  margin: 0;
  font-weight: 400;
  word-break: break-word;
`;
const StyledEditableText = styled(EditableText)`
  margin: 0;
  font-weight: 400;
  word-break: break-word;
`;
const MiddleRow = styled("div")`
  display: flex;
  width: 100%;
  justify-content: space-between;
  line-height: 24px;
`;

const StyledFaIcon = styled(FaIcon)`
  margin: 0 6px;
`;

const StyledWithTooltip = styled(WithTooltip)`
  margin-left: 10px;
`;

export interface FormConfigDragItem {
  width: number;
  height: number;
  type: string;
  id: string;
  label: string;
}

interface PropsT {
  datasetId: DatasetIdT;
  config: FormConfigT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
}

const FormConfig: React.FC<PropsT> = ({
  datasetId,
  config,
  onIndicateDeletion,
  onIndicateShare,
}) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const formLabel = useFormLabelByType(config.formType);
  const availableTags = useSelector<StateT, string[]>(
    (state) => state.formConfigs.tags
  );

  const createdAt = config.createdAt
    ? formatDateDistance(parseISO(config.createdAt), new Date(), true)
    : "";

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);
  const [isEditingTags, setIsEditingTags] = useState<boolean>(false);

  const label = config.label || config.id.toString();
  const isLabelHighlighted = useIsLabelHighlighted(label);

  const mayEdit = config.own || config.shared;
  const isNotEditing = !(isEditingLabel || isEditingTags);

  const dispatch = useDispatch();

  const patchFormConfig = usePatchFormConfig();

  const onPatchFormConfig = async (
    attributes: {
      shared?: boolean;
      label?: string;
      tags?: string[];
    },
    errorMessageKey: string
  ) => {
    setIsLoading(true);
    try {
      await patchFormConfig(datasetId, config.id, attributes);

      dispatch(patchFormConfigSuccess(config.id, attributes));
    } catch (e) {
      dispatch(setMessage(errorMessageKey));
    }
    setIsLoading(false);
  };

  const onRenameFormConfig = async (label: string) => {
    await onPatchFormConfig({ label }, "formConfig.renameError");

    setIsEditingLabel(false);
  };

  const onRetagFormConfig = async (tags: string[]) => {
    await onPatchFormConfig({ tags }, "formConfig.retagError");

    setIsEditingTags(false);
  };

  const item: FormConfigDragItem = {
    height: 0,
    width: 0,
    type: FORM_CONFIG,
    id: config.id,
    label: config.label,
  };
  const [, drag] = useDrag({
    item,
    begin: (): FormConfigDragItem => ({
      ...item,
      ...getWidthAndHeight(ref),
    }),
  });

  return (
    <Root
      ref={(instance) => {
        ref.current = instance;

        if (isNotEditing) {
          drag(instance);
        }
      }}
      own={!!config.own}
      shared={!!config.shared}
      system={!!config.system || (!config.own && !config.shared)}
    >
      <TopInfos>
        <div>
          {formLabel}
          {config.own && config.shared && (
            <SharedIndicator onClick={onIndicateShare}>
              {T.translate("common.shared")}
            </SharedIndicator>
          )}
          <TopRight>
            {createdAt}
            {mayEdit &&
              !isEditingTags &&
              (!config.tags || config.tags.length === 0) && (
                <StyledWithTooltip text={T.translate("common.addTag")}>
                  <IconButton
                    icon="tags"
                    bare
                    onClick={() => setIsEditingTags(!isEditingTags)}
                  />
                </StyledWithTooltip>
              )}
            {config.own && !config.shared && (
              <StyledWithTooltip text={T.translate("common.share")}>
                <IconButton icon="upload" bare onClick={onIndicateShare} />
              </StyledWithTooltip>
            )}
            {isLoading ? (
              <StyledFaIcon icon="spinner" />
            ) : (
              config.own && (
                <StyledWithTooltip text={T.translate("common.delete")}>
                  <IconButton icon="times" bare onClick={onIndicateDeletion} />
                </StyledWithTooltip>
              )
            )}
          </TopRight>
        </div>
      </TopInfos>
      <MiddleRow>
        {mayEdit ? (
          <StyledEditableText
            isHighlighted={isLabelHighlighted}
            loading={isLoading}
            text={label}
            selectTextOnMount={true}
            editing={isEditingLabel}
            onSubmit={onRenameFormConfig}
            onToggleEdit={() => setIsEditingLabel(!isEditingLabel)}
          />
        ) : (
          <StyledSelectableLabel label={label} />
        )}
        <MiddleRight>{config.ownerName}</MiddleRight>
      </MiddleRow>
      {mayEdit ? (
        <EditableTags
          tags={config.tags}
          editing={isEditingTags}
          loading={isLoading}
          onSubmit={onRetagFormConfig}
          onToggleEdit={() => setIsEditingTags(!isEditingTags)}
          tagComponent={<FormConfigTags tags={config.tags} />}
          availableTags={availableTags}
        />
      ) : (
        <FormConfigTags tags={config.tags} />
      )}
    </Root>
  );
};

export default FormConfig;
