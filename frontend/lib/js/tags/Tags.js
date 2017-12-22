import React                from 'react';
import PropTypes            from 'prop-types';
import Tag                  from './Tag';

export const Tags = (props) => {
  return !!props.tags && props.tags.length > 0 && (
    <div className={props.className}>
      {
        props.tags.map((tag, i) => (
          <Tag
            key={i}
            label={tag.label}
            isSelected={tag.isSelected}
            onClick={() => props.onClickTag(tag.label)}
          />
        ))
      }
    </div>
  );
};

Tags.propTypes = {
  className: PropTypes.string,
  tags: PropTypes.arrayOf(PropTypes.shape({
    label: PropTypes.string.isRequired,
    isSelected: PropTypes.bool,
  })),
  onClickTag: PropTypes.func.isRequired,
};

export default Tags;
