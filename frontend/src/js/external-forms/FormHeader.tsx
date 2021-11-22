import styled from "@emotion/styled";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  width: 100%;
`;

const Description = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.md};
`;

interface Props {
  description: string;
  className?: string;
}

const FormHeader = ({ className, description }: Props) => {
  return (
    <Root className={className}>
      <Description>{description}</Description>
    </Root>
  );
};

export default FormHeader;
