import styled from "@emotion/styled";

interface Props {
  className?: string;
  message: string;
}

const Root = styled("p")`
  color: ${({ theme }) => theme.col.red};
  font-weight: 400;
`;

const ErrorMessage = ({ className, message }: Props) => {
  return <Root className={className}>{message}</Root>;
};

export default ErrorMessage;
