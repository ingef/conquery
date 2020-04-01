import React, { useState } from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/react";

import T from "i18n-react";
import { useDrag } from "react-dnd";
import { useSelector, useDispatch } from "react-redux";
import { parseISO } from "date-fns";

import ErrorMessage from "../../error-message/ErrorMessage";
import { FORM_CONFIG } from "../../common/constants/dndTypes";
import SelectableLabel from "../../selectable-label/SelectableLabel";

import IconButton from "../../button/IconButton";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";

import EditableText from "../../form-components/EditableText";
import EditableTags from "../../form-components/EditableTags";

// import { deletePreviousQueryModalOpen } from "../delete-modal/actions";

// import PreviousQueryTags from "./PreviousQueryTags";
import { formatDateDistance } from "../../common/helpers";
import { FormConfigT } from "./reducer";
import { StateT } from "app-types";
import { DatasetIdT } from "js/api/types";
import { patchFormConfig } from "js/api/api";
import FormConfigTags from "./FormConfigTags";
import { patchFormConfigSuccess } from "./actions";
import { setMessage } from "js/snack-message/actions";

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
const StyledErrorMessage = styled(ErrorMessage)`
  margin: 0;
`;

const StyledFaIcon = styled(FaIcon)`
  margin: 0 6px;
`;

const StyledWithTooltip = styled(WithTooltip)`
  margin-left: 10px;
`;

interface PropsT {
  datasetId: DatasetIdT;
  config: FormConfigT;
}

const FormConfig: React.FC<PropsT> = ({ datasetId, config }) => {
  const availableTags = useSelector<StateT, string[]>(
    state => state.formConfigs.tags
  );

  const createdAt = formatDateDistance(
    parseISO(config.createdAt),
    new Date(),
    true
  );

  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isEditingLabel, setIsEditingLabel] = useState<boolean>(false);
  const [isEditingTags, setIsEditingTags] = useState<boolean>(false);

  const label = config.label || config.id.toString();
  const mayEdit = config.own || config.shared;
  const isNotEditing = !(isEditingLabel || isEditingTags);

  const dispatch = useDispatch();

  const onSetSharedFormConfig = async (shared: boolean) => {
    setIsLoading(true);
    try {
      await patchFormConfig(datasetId, config.id, { shared });

      dispatch(patchFormConfigSuccess(config.id, { shared }));
    } catch (e) {
      dispatch(setMessage("formConfig.shareError"));
    }
    setIsLoading(false);
  };

  const onRenameFormConfig = (label: string) => {
    dispatch(renameFormConfig(datasetId, config.id, label));
  };

  const onRetagFormConfig = (tags: string[]) => {
    dispatch(retagFormConfig(datasetId, config.id, tags));
  };
  const onDeleteFormConfig = () => {
    dispatch(deleteFormConfigModalOpen(config.id));
  };

  const [collectedProps, drag] = useDrag({
    item: {
      // TODO: Try to actually measure this using ref + getBoundingClientRect
      width: 200,
      height: 100,
      type: FORM_CONFIG,
      id: config.id,
      label: config.label
    },
    isDragging: monitor => monitor.isDragging()
  });

  return (
    <Root
      ref={instance => {
        if (isNotEditing) drag(instance);
      }}
      own={!!config.own}
      shared={!!config.shared}
      system={!!config.system || (!config.own && !config.shared)}
    >
      <TopInfos>
        <div>
          {config.own && config.shared && (
            <SharedIndicator
              onClick={() => onSetSharedFormConfig(!config.shared)}
            >
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
                <IconButton
                  icon="upload"
                  bare
                  onClick={() => onSetSharedFormConfig(!config.shared)}
                />
              </StyledWithTooltip>
            )}
            {isLoading ? (
              <StyledFaIcon icon="spinner" />
            ) : (
              config.own && (
                <StyledWithTooltip text={T.translate("common.delete")}>
                  <IconButton icon="times" bare onClick={onDeleteFormConfig} />
                </StyledWithTooltip>
              )
            )}
          </TopRight>
        </div>
      </TopInfos>
      <MiddleRow>
        {mayEdit ? (
          <StyledEditableText
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
        <Gray>{config.ownerName}</Gray>
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
      {!!config.error && <StyledErrorMessage message={config.error} />}
    </Root>
  );
};

export default FormConfig;
