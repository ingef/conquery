import * as React from "react";
import styled from "@emotion/styled";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";
import T from "i18n-react";

import Modal from "../modal/Modal";
import InputSelect from "../form-components/InputSelect";
import InputText from "../form-components/InputText";
import ScrollableList from "../scrollable-list/ScrollableList";
import PrimaryButton from "../button/PrimaryButton";
import FaIcon from "../icon/FaIcon";

import type { StateT } from "../app/reducers";
import type { DatasetIdT } from "../api/types";
import type { TreesT } from "../concept-trees/reducer";

import { selectConceptRootNodeAndResolveCodes } from "./actions";

const Root = styled("div")`
  padding: 0 0 10px;
`;

const Section = styled("div")`
  margin-top: 15px;
  padding: 15px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.1);
  display: grid;
  grid-gap: 20px;
`;

const Msg = styled("p")`
  margin: 0;
`;
const MsgRow = styled("div")`
  display: flex;
  align-items: flex-end;
`;

const BigIcon = styled(FaIcon)`
  font-size: 20px;
  margin-right: 10px;
`;
const ErrorIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.red};
`;
const SuccessIcon = styled(BigIcon)`
  color: ${({ theme }) => theme.col.green};
`;
const CenteredIcon = styled(FaIcon)`
  text-align: center;
`;
const SxPrimaryButton = styled(PrimaryButton)`
  margin-left: 15px;
  flex-shrink: 0;
`;

type PropsType = {
  loading: boolean;
  filename: string;
  availableConceptRootNodes: Object[];
  selectedConceptRootNode: Object;
  selectedDatasetId: DatasetIdT;
  conceptCodesFromFile: string[];
  resolved: Object;
  rootConcepts: TreesT;
  resolvedItemsCount: number;
  unresolvedItemsCount: number;
  error: Object;
  onSelectConceptRootNode: Function;

  // This really comes from outside container, and depends on the context
  // in which this modal is opened. (query editor / statistic form field)
  onAccept: Function;
  onClose: Function;
};

const UploadConceptListModal = (props: PropsType) => {
  const [label, setLabel] = React.useState(props.filename);

  React.useEffect(() => {
    setLabel(props.filename);
  }, [props.filename]);

  const {
    availableConceptRootNodes,
    selectedConceptRootNode,
    selectedDatasetId,
    loading,
    conceptCodesFromFile,
    resolved,
    resolvedItemsCount,
    unresolvedItemsCount,
    error,
    rootConcepts,
    onSelectConceptRootNode,

    onAccept,
    onClose
  } = props;

  if (!conceptCodesFromFile || conceptCodesFromFile.length === 0) {
    onClose();
  }

  const hasUnresolvedItems = unresolvedItemsCount > 0;
  const hasResolvedItems = resolvedItemsCount > 0;

  const onSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    onAccept(label, rootConcepts, resolved.resolvedConcepts);
    onClose();
  };

  return (
    <Modal
      closeIcon
      onClose={onClose}
      headline={T.translate("uploadConceptListModal.headline")}
    >
      <Root>
        <InputSelect
          label={T.translate("uploadConceptListModal.selectConceptRootNode")}
          input={{
            value: selectedConceptRootNode,
            onChange: value =>
              onSelectConceptRootNode(
                selectedDatasetId,
                value,
                conceptCodesFromFile
              )
          }}
          options={availableConceptRootNodes.map(x => ({
            value: x.key,
            label: x.value.label
          }))}
          selectProps={{
            isSearchable: true,
            autoFocus: true
          }}
        />
        {!!resolved && !hasResolvedItems && !hasUnresolvedItems && (
          <Section>
            <Msg>{T.translate("uploadConceptListModal.nothingResolved")}</Msg>
          </Section>
        )}
        {(!!error ||
          !!loading ||
          (!!resolved && (hasResolvedItems || hasUnresolvedItems))) && (
          <Section>
            {error && (
              <p>
                <ErrorIcon icon="exclamation-circle" />
                {T.translate("uploadConceptListModal.error")}
              </p>
            )}
            {loading && <CenteredIcon icon="spinner" />}
            {resolved && (
              <>
                {hasResolvedItems && (
                  <form onSubmit={onSubmit}>
                    <Msg>
                      <SuccessIcon icon="check-circle" />
                      {T.translate("uploadConceptListModal.resolvedCodes", {
                        context: resolvedItemsCount
                      })}
                    </Msg>
                    <MsgRow>
                      <InputText
                        label={T.translate("uploadConceptListModal.label")}
                        fullWidth
                        inputProps={{
                          autoFocus: true
                        }}
                        input={{
                          value: label,
                          onChange: setLabel
                        }}
                      />
                      <SxPrimaryButton type="submit">
                        {T.translate("uploadConceptListModal.insertNode")}
                      </SxPrimaryButton>
                    </MsgRow>
                  </form>
                )}
                {hasUnresolvedItems && (
                  <div>
                    <Msg>
                      <ErrorIcon icon="exclamation-circle" />
                      <span>
                        {T.translate("uploadConceptListModal.unknownCodes", {
                          context: unresolvedItemsCount
                        })}
                      </span>
                    </Msg>
                    <ScrollableList
                      maxVisibleItems={3}
                      fullWidth
                      items={resolved.unknownCodes}
                    />
                  </div>
                )}
              </>
            )}
          </Section>
        )}
      </Root>
    </Modal>
  );
};

const selectUnresolvedItemsCount = state => {
  const { resolved } = state.uploadConceptListModal;

  return resolved && resolved.unknownCodes && resolved.unknownCodes.length
    ? resolved.unknownCodes.length
    : 0;
};

const selectResolvedItemsCount = state => {
  const { resolved } = state.uploadConceptListModal;

  return resolved &&
    resolved.resolvedConcepts &&
    resolved.resolvedConcepts.length
    ? resolved.resolvedConcepts.length
    : 0;
};

const selectAvailableConceptRootNodes = state => {
  const { trees } = state.conceptTrees;

  if (!trees) return null;

  return Object.entries(trees)
    .map(([key, value]) => ({ key, value }))
    .filter(({ key, value }) => value.codeListResolvable)
    .sort((a, b) =>
      a.value.label.toLowerCase().localeCompare(b.value.label.toLowerCase())
    );
};

const mapStateToProps = (state: StateT) => ({
  filename: state.uploadConceptListModal.filename,
  conceptCodesFromFile: state.uploadConceptListModal.conceptCodesFromFile,
  availableConceptRootNodes: selectAvailableConceptRootNodes(state),
  selectedConceptRootNode: state.uploadConceptListModal.selectedConceptRootNode,
  loading: state.uploadConceptListModal.loading,
  resolved: state.uploadConceptListModal.resolved,
  resolvedItemsCount: selectResolvedItemsCount(state),
  unresolvedItemsCount: selectUnresolvedItemsCount(state),
  rootConcepts: state.conceptTrees.trees,
  error: state.uploadConceptListModal.error
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  onSelectConceptRootNode: (...params) =>
    dispatch(selectConceptRootNodeAndResolveCodes(...params))
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(UploadConceptListModal);
