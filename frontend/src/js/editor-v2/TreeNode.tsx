import { faCalendarMinus } from "@fortawesome/free-regular-svg-icons";
import { createId } from "@paralleldrive/cuid2";
import { type DOMAttributes, memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { DNDType } from "../common/constants/dndTypes";
import FaIcon from "../icon/FaIcon";
import { nodeIsConceptQueryNode, useActiveState } from "../model/node";
import { getRootNodeLabel } from "../standard-query-editor/helper";
import type {
  DragItemConceptTreeNode,
  DragItemQuery,
} from "../standard-query-editor/types";
import Dropzone, {
  type DropzoneProps,
  type PossibleDroppableObject,
} from "../ui-components/Dropzone";
import WithTooltip from "../ui-components/WithTooltip";
import { EDITOR_DROP_TYPES } from "./config";
import { DateRange } from "./date-restriction/DateRange";
import { Connector, Grid } from "./EditorLayout";
import { TreeNodeConcept } from "./TreeNodeConcept";
import { TimeConnection } from "./time-connection/TimeConnection";
import type { ConnectionKind, Tree } from "./types";
import { useGetTranslatedConnection } from "./util";

const nodeContainer = tv({
  base: ["grid", "gap-[5px]"],
});

const node = tv({
  base: [
    "flex flex-col",
    "gap-[10px]",
    "p-6",
    "w-[inherit]",
    "rounded",
    "border-2 border-gray-400",
    "bg-bg-50",
    "cursor-pointer",
  ],
  variants: {
    // later wins when several are set
    isDragging: { true: "p-[5px]" },
    leaf: { true: ["px-[10px] py-2", "w-[230px]", "bg-white"] },
    selected: {
      true: [
        "border-gray-500",
        "shadow-[inset_0px_0px_0px_4px_var(--color-primary-50)]",
      ],
    },
    active: { true: "border-primary-500" },
    negated: { true: "border-red" },
  },
});

function getGridStyles(tree: Tree) {
  if (!tree.children) {
    return {};
  }

  if (tree.children.direction === "horizontal") {
    return {
      gridAutoFlow: "column",
    };
  } else {
    return {
      gridTemplateColumns: "1fr",
    };
  }
}

const invisibleDropzone = tv({
  base: ["h-full w-full", "p-5"],
  variants: {
    bare: { true: "p-[6px]" },
  },
});

const InvisibleDropzoneContainer = ({
  className,
  ...props
}: DropzoneProps<PossibleDroppableObject> & { className?: string }) => (
  <Dropzone
    className={invisibleDropzone({ bare: props.bare, className })}
    {...props}
  />
);

const InvisibleDropzone = (
  props: Omit<DropzoneProps<unknown>, "acceptedDropTypes">,
) => {
  return (
    <InvisibleDropzoneContainer
      invisible
      naked
      acceptedDropTypes={EDITOR_DROP_TYPES}
      {...props}
    />
  );
};

const previousQueryLabel = tv({
  base: [
    "leading-[1.2]",
    "text-xs",
    "uppercase",
    "font-bold",
    "text-primary-500",
  ],
});

const rootNode = tv({
  base: [
    "leading-none",
    "text-xs",
    "uppercase",
    "font-bold",
    "text-primary-500",
    "[word-break:break-word]",
  ],
});

const dates = tv({
  base: ["text-right", "text-xs", "uppercase", "font-normal"],
});

export function TreeNode({
  tree,
  treeParent,
  updateTreeNode,
  droppable,
  selectedNode,
  setSelectedNodeId,
  featureContentInfos,
  onOpenQueryNodeEditor,
  onOpenTimeModal,
  onRotateConnector,
}: {
  tree: Tree;
  treeParent?: Tree;
  updateTreeNode: (id: string, update: (node: Tree) => void) => void;
  droppable: {
    h: boolean;
    v: boolean;
  };
  selectedNode: Tree | undefined;
  setSelectedNodeId: (id: Tree["id"] | undefined) => void;
  featureContentInfos?: boolean;
  onOpenQueryNodeEditor?: () => void;
  onOpenTimeModal?: () => void;
  onRotateConnector?: () => void;
}) {
  const gridStyles = getGridStyles(tree);

  const { t } = useTranslation();

  const rootNodeLabel = tree.data ? getRootNodeLabel(tree.data) : null;

  const { active, tooltipText } = useActiveState(tree.data);

  const onDropOutsideOfNode = ({
    pos,
    direction,
    item,
  }: {
    direction: "h" | "v";
    pos: "b" | "a";
    item: unknown;
  }) => {
    // Create a new "parent" and create a new "item", make parent contain tree and item
    const newParentId = createId();
    const newItemId = createId();

    updateTreeNode(tree.id, (node) => {
      const newChildren: Tree[] = [
        {
          id: newItemId,
          negation: false,
          data: item as DragItemQuery | DragItemConceptTreeNode | undefined,
          parentId: newParentId,
        },
        {
          ...tree,
          parentId: newParentId,
        },
      ];

      node.id = newParentId;
      node.data = undefined;
      node.dates = undefined;
      node.negation = false;

      const connection =
        treeParent?.children?.connection || tree.children?.connection;

      node.children = {
        connection: connection === "and" ? "or" : "and",
        direction: direction === "h" ? "horizontal" : "vertical",
        items: pos === "b" ? newChildren : newChildren.reverse(),
      };
    });
    setSelectedNodeId(newItemId);
  };

  const onDropAtChildrenIdx = ({
    idx,
    item,
  }: {
    idx: number;
    item: unknown;
  }) => {
    const newItemId = createId();
    // Create a new "item" and insert it at idx of tree.children
    updateTreeNode(tree.id, (node) => {
      if (node.children) {
        node.children.items.splice(idx, 0, {
          id: newItemId,
          negation: false,
          data: item as DragItemQuery | DragItemConceptTreeNode | undefined,
          parentId: node.id,
        });
      }
    });
    setSelectedNodeId(newItemId);
  };

  return (
    <div className={nodeContainer()}>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) =>
            onDropOutsideOfNode({ pos: "b", direction: "v", item })
          }
        />
      )}
      <div
        className={nodeContainer()}
        style={{
          gridAutoFlow: "column",
        }}
      >
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) =>
              onDropOutsideOfNode({
                pos: "b",
                direction: "h",
                item,
              })
            }
          />
        )}
        <Dropzone
          naked
          bare
          acceptedDropTypes={EDITOR_DROP_TYPES}
          onDrop={() => {}}
        >
          {({ canDrop }) => (
            <WithTooltip text={tooltipText}>
              {/* biome-ignore lint/a11y/noStaticElementInteractions: TODO node selection area, emotion had hidden this */}
              {/* biome-ignore lint/a11y/useKeyWithClickEvents: see above */}
              <div
                className={node({
                  isDragging: canDrop,
                  active,
                  negated: tree.negation,
                  leaf: !tree.children,
                  selected: selectedNode?.id === tree.id,
                })}
                onDoubleClick={(e) => {
                  if (tree.data && nodeIsConceptQueryNode(tree.data)) {
                    e.stopPropagation();
                    onOpenQueryNodeEditor?.();
                  }
                }}
                onClick={(e) => {
                  e.stopPropagation();
                  setSelectedNodeId(tree.id);
                }}
              >
                {tree.children && tree.children.connection === "time" && (
                  <TimeConnection
                    conditions={tree.children}
                    onDoubleClick={(e) => {
                      e.stopPropagation();
                      onOpenTimeModal?.();
                    }}
                  />
                )}
                {tree.dates?.restriction && (
                  <div className={dates()}>
                    <DateRange dateRange={tree.dates.restriction} />
                  </div>
                )}
                {tree.dates?.excluded && (
                  <div className={dates()}>
                    <FaIcon red icon={faCalendarMinus} left />
                    {t("editorV2.datesExcluded")}
                  </div>
                )}
                {(!tree.children || tree.data) && (
                  <div className="flex flex-col gap-1">
                    {tree.data?.type !== DNDType.CONCEPT_TREE_NODE && (
                      <p className={previousQueryLabel()}>
                        {t("queryEditor.previousQuery")}
                      </p>
                    )}
                    {rootNodeLabel && (
                      <p className={rootNode()}>{rootNodeLabel}</p>
                    )}
                    {tree.data?.label && (
                      <div className="text-sm font-normal text-gray-800">
                        {tree.data.label}
                      </div>
                    )}
                    {tree.data && nodeIsConceptQueryNode(tree.data) && (
                      <TreeNodeConcept
                        node={tree.data}
                        featureContentInfos={featureContentInfos}
                      />
                    )}
                  </div>
                )}
                {tree.children && (
                  <Grid style={gridStyles}>
                    <InvisibleDropzone
                      key="dropzone-before"
                      naked
                      bare
                      onDrop={(item) => onDropAtChildrenIdx({ idx: 0, item })}
                    >
                      {() => (
                        <Connection connection={tree.children?.connection} />
                      )}
                    </InvisibleDropzone>
                    {tree.children.items.map((item, i, items) => (
                      <>
                        <TreeNode
                          key={item.id}
                          featureContentInfos={featureContentInfos}
                          tree={item}
                          treeParent={tree}
                          updateTreeNode={updateTreeNode}
                          selectedNode={selectedNode}
                          setSelectedNodeId={setSelectedNodeId}
                          onOpenQueryNodeEditor={onOpenQueryNodeEditor}
                          onOpenTimeModal={onOpenTimeModal}
                          onRotateConnector={onRotateConnector}
                          droppable={{
                            h:
                              !item.children &&
                              tree.children?.direction === "vertical",
                            v:
                              !item.children &&
                              tree.children?.direction === "horizontal",
                          }}
                        />
                        {i < items.length - 1 && (
                          <InvisibleDropzoneContainer
                            key={`${item.id}connector`}
                            acceptedDropTypes={[DNDType.CONCEPT_TREE_NODE]}
                            naked
                            bare
                            transparent
                            onDrop={(item) =>
                              onDropAtChildrenIdx({ idx: i + 1, item })
                            }
                          >
                            {() => (
                              <Connection
                                onDoubleClick={(e) => {
                                  e.stopPropagation();
                                  onRotateConnector?.();
                                }}
                                connection={tree.children?.connection}
                              />
                            )}
                          </InvisibleDropzoneContainer>
                        )}
                      </>
                    ))}
                    <InvisibleDropzone
                      key="dropzone-after"
                      naked
                      bare
                      onDrop={(item) =>
                        onDropAtChildrenIdx({
                          idx: tree.children!.items.length,
                          item,
                        })
                      }
                    >
                      {() => (
                        <Connection connection={tree.children?.connection} />
                      )}
                    </InvisibleDropzone>
                  </Grid>
                )}
              </div>
            </WithTooltip>
          )}
        </Dropzone>
        {droppable.h && (
          <InvisibleDropzone
            onDrop={(item) =>
              onDropOutsideOfNode({ pos: "a", direction: "h", item })
            }
          />
        )}
      </div>
      {droppable.v && (
        <InvisibleDropzone
          onDrop={(item) =>
            onDropOutsideOfNode({ pos: "a", direction: "v", item })
          }
        />
      )}
    </div>
  );
}

const Connection = memo(
  ({
    connection,
    onDoubleClick,
  }: {
    connection?: ConnectionKind;
    onDoubleClick?: DOMAttributes<HTMLElement>["onDoubleClick"];
  }) => {
    const getTranslatedConnection = useGetTranslatedConnection();

    return (
      <Connector onDoubleClick={onDoubleClick}>
        {getTranslatedConnection(connection)}
      </Connector>
    );
  },
);
