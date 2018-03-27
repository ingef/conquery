// @flow

import React                      from 'react';
import { type FieldPropsType }    from 'redux-form';

import {
  resetAllFiltersInTables
}                                 from '../../model/table';
import { nodeHasActiveFilters }   from '../../model/node';

import {
  DynamicInputGroup,
  DropzoneList,
}                                 from '../../form-components';

import {
  CATEGORY_TREE_NODE
}                                 from '../../common/constants/dndTypes';

import { FormQueryNodeEditor }    from '../form-query-node-editor';

import FormConceptNode            from './FormConceptNode';
import FormConceptNodeDropzone    from './FormConceptNodeDropzone';

type PropsType = FieldPropsType & {
  name: string,
  label: string,
  datasetId: string,
};

const addValue = (value, newValue) => [ ...value, newValue ];

const removeValue = (value, valueIdx) => {
  return [
    ...value.slice(0, valueIdx),
    ...value.slice(valueIdx + 1),
  ];
};

const addConcept = (value, valueIdx, item) => ([
  ...value.slice(0, valueIdx),
  {
    ...value[valueIdx],
    concepts: [
      ...value[valueIdx].concepts,
      item,
    ]
  },
  ...value.slice(valueIdx + 1)
]);

const removeConcept = (value, valueIdx, conceptIdx) => (
  [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...value[valueIdx].concepts.slice(0, conceptIdx),
        ...value[valueIdx].concepts.slice(conceptIdx + 1),
      ]
    },
    ...value.slice(valueIdx + 1)
  ]
);

const setConcept = (value, valueIdx, conceptIdx, item) => (
  [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...value[valueIdx].concepts.slice(0, conceptIdx),
        item,
        ...value[valueIdx].concepts.slice(conceptIdx + 1),
      ]
    },
    ...value.slice(valueIdx + 1)
  ]
);

const setConceptProperties = (value, valueIdx, conceptIdx, props) =>
  setConcept(value, valueIdx, conceptIdx, { ...value[valueIdx].concepts[conceptIdx], ...props });

const toggleTable = (value, valueIdx, conceptIdx, tableIdx, isExcluded) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...concepts.slice(0, conceptIdx),
        {
          ...concepts[conceptIdx],
          tables: [
            ...tables.slice(0, tableIdx),
            {
              ...tables[tableIdx],
              exclude: isExcluded
            },
            ...tables.slice(tableIdx + 1),
          ]
        },
        ...concepts.slice(conceptIdx + 1),
      ]
    },
    ...value.slice(valueIdx + 1)
  ];
};

const setFilterValue = (
  value,
   valueIdx,
   conceptIdx,
   tableIdx,
   filterIdx,
   filterValue,
   formattedFilterValue
) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const filters = tables[tableIdx].filters;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: [
      ...tables.slice(0, tableIdx),
      {
        ...tables[tableIdx],
        filters: [
          ...filters.slice(0, filterIdx),
          {
            ...filters[filterIdx],
            value: filterValue,
            formattedValue: formattedFilterValue,
          },
          ...filters.slice(filterIdx + 1),
        ]
      },
      ...tables.slice(tableIdx + 1),
    ]
  });
}

const resetAllFilters = (value, valueIdx, conceptIdx) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;

  return setConceptProperties(value, valueIdx, conceptIdx, {
    tables: resetAllFiltersInTables(tables)
  });
}

const switchFilterMode = (value, valueIdx, conceptIdx, tableIdx, filterIdx, mode) => {
  const concepts = value[valueIdx].concepts;
  const tables = concepts[conceptIdx].tables;
  const filters = tables[tableIdx].filters;

  return [
    ...value.slice(0, valueIdx),
    {
      ...value[valueIdx],
      concepts: [
        ...concepts.slice(0, conceptIdx),
        {
          ...concepts[conceptIdx],
          tables: [
            ...tables.slice(0, tableIdx),
            {
              ...tables[tableIdx],
              filters: [
                ...filters.slice(0, filterIdx),
                {
                  ...filters[filterIdx],
                  mode: mode,
                  value: null
                },
                ...filters.slice(filterIdx + 1),
              ]
            },
            ...tables.slice(tableIdx + 1),
          ]
        },
        ...concepts.slice(conceptIdx + 1),
      ]
    },
    ...value.slice(valueIdx + 1)
  ];
}

export const FormConceptGroup = (props: PropsType) => (
  <div className="externalForms__concept-group">
    <DropzoneList
      label={props.label}
      className="externalForms__dropzone-list"
      itemClassName="externalForms__dropzone-list__item"
      dropzoneClassName="externalForms__dropzone"
      dropzoneText={props.attributeDropzoneText}
      acceptedDropTypes={[CATEGORY_TREE_NODE]}
      onDelete={(i) => props.input.onChange(removeValue(props.input.value, i))}
      onDrop={(dropzoneProps, monitor) => {
        const item = monitor.getItem();

        return props.input.onChange(
          addConcept(
            addValue(props.input.value, props.newValue),
            props.input.value.length, // Assuming the last index has increased after addValue
            item
          )
        );
      }}
      items={
        props.input.value.map((row, i) =>
          <div>
            { props.renderRowPrefix ? props.renderRowPrefix(props.input, row, i) : null }
            <DynamicInputGroup
              className="externalForms__concept-group__input-group"
              key={i}
              onAddClick={() => props.input.onChange(addConcept(props.input.value, i, null))}
              onRemoveClick={j => props.input.onChange(
                props.input.value && props.input.value[i].concepts.length === 1
                ? removeValue(props.input.value, i)
                : removeConcept(props.input.value, i, j))
              }
              items={
                row.concepts.map((concept, j) =>
                  concept
                  ? <FormConceptNode
                      key={j}
                      valueIdx={i}
                      conceptIdx={j}
                      conceptNode={concept}
                      name={props.name}
                      hasActiveFilters={nodeHasActiveFilters(concept)}
                      onFilterClick={() =>
                        props.input.onChange(
                          setConceptProperties(props.input.value, i, j, { isEditing: true })
                        )
                      }
                    />
                  : <FormConceptNodeDropzone
                      dropzoneText={props.conceptDropzoneText}
                      onDrop={(dropzoneProps, monitor) => {
                        const item = monitor.getItem();

                        return props.input.onChange(setConcept(props.input.value, i, j, item));
                      }}
                    />
                )
              }
            />
          </div>
        )
      }
    />
    <FormQueryNodeEditor
      formType={props.formType}
      fieldName={props.name}
      datasetId={props.datasetId}
      onCloseModal={(valueIdx, conceptIdx) =>
        props.input.onChange(
          setConceptProperties(props.input.value, valueIdx, conceptIdx, { isEditing: false })
        )
      }
      onUpdateLabel={(valueIdx, conceptIdx, label) =>
        props.input.onChange(
          setConceptProperties(props.input.value, valueIdx, conceptIdx, { label })
        )
      }
      onDropConcept={(valueIdx, conceptIdx, concept) => {
        const node = props.input.value[valueIdx].concepts[conceptIdx];
        props.input.onChange(
          setConceptProperties(props.input.value, valueIdx, conceptIdx, {
            ids: [...concept.ids, ...node.ids]
          })
        )
      }}
      onRemoveConcept={(valueIdx, conceptIdx, conceptId) => {
        const node = props.input.value[valueIdx].concepts[conceptIdx];
        props.input.onChange(
          setConceptProperties(props.input.value, valueIdx, conceptIdx, {
            ids: node.ids.filter(id => id !== conceptId)
          })
        )
      }}
      onToggleTable={(valueIdx, conceptIdx, tableIdx, isExcluded) =>
        props.input.onChange(
          toggleTable(props.input.value, valueIdx, conceptIdx, tableIdx, isExcluded)
        )
      }
      onSetFilterValue={(
        valueIdx,
        conceptIdx,
        tableIdx,
        filterIdx,
        filterValue,
        formattedFilterValue
      ) =>
        props.input.onChange(
          setFilterValue(
            props.input.value,
            valueIdx,
            conceptIdx,
            tableIdx,
            filterIdx,
            filterValue,
            formattedFilterValue
          )
        )
      }
      onSwitchFilterMode={(valueIdx, conceptIdx, tableIdx, filterIdx, mode) =>
        props.input.onChange(
          switchFilterMode(props.input.value, valueIdx, conceptIdx, tableIdx, filterIdx, mode)
        )
      }
      onResetAllFilters={(valueIdx, conceptIdx) =>
        props.input.onChange(
          resetAllFilters(props.input.value, valueIdx, conceptIdx)
        )
      }
    />
  </div>
);
