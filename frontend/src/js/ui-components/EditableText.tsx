import styled from "@emotion/styled";
import { faPen } from "@fortawesome/free-solid-svg-icons";
import Highlighter from "react-highlight-words";

import IconButton from "../button/IconButton";
import HighlightableLabel from "../highlightable-label/HighlightableLabel";
import WithTooltip from "../tooltip/WithTooltip";

import EditableTextForm from "./EditableTextForm";

const SxIconButton = styled(IconButton)`
  margin-right: ${({ large }) => (large ? "10px" : "8px")};
  padding: 2px 0;
`;

const Text = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const SxHighlightableLabel = styled(HighlightableLabel)`
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

const EditableText = ({
  className,
  loading,
  editing,
  text,
  tooltip,
  large,
  saveOnClickoutside,
  isHighlighted,
  highlightedWords,
  selectTextOnMount,
  onSubmit,
  onToggleEdit,
}: {
  className?: string;
  loading?: boolean;
  editing: boolean;
  text: string;
  tooltip?: string;
  large?: boolean;
  saveOnClickoutside?: boolean;
  isHighlighted?: boolean;
  highlightedWords?: string[];
  selectTextOnMount?: boolean;
  onSubmit: (text: string) => void;
  onToggleEdit: () => void;
}) => {
  return editing ? (
    <EditableTextForm
      className={className}
      loading={loading}
      text={text}
      selectTextOnMount={selectTextOnMount}
      saveOnClickoutside={saveOnClickoutside}
      onSubmit={onSubmit}
      onCancel={onToggleEdit}
    />
  ) : (
    <Text className={className}>
      <WithTooltip text={tooltip}>
        <SxIconButton
          bare
          icon={faPen}
          onClick={onToggleEdit}
          small
          large={large}
        />
      </WithTooltip>
      <SxHighlightableLabel isHighlighted={isHighlighted}>
        {highlightedWords && highlightedWords.length > 0 ? (
          <Highlighter
            searchWords={highlightedWords}
            autoEscape
            textToHighlight={text}
          />
        ) : (
          text
        )}
      </SxHighlightableLabel>
    </Text>
  );
};

export default EditableText;
