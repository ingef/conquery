import styled from "@emotion/styled"

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  gap: 10px;
`

const Key = styled("span")`
  font-weight: bold;
  margin-left: 10px;

  &:first-of-type {
    margin-left: 0;
  }
`

export default function HeadlineStats() {
  return (
    <Root>
      <Key>Zeilen:</Key>
      xxx
      <Key>Min Datum:</Key>
      XX.XX.XXXX
      <Key>Max Datum:</Key>
      XX.XX.XXXX
      <Key>Darumgsbereich:</Key>
      XX.XX.XXXX - XX.XX.XXXX
      <Key>Fehlende Werte:</Key>
      xxx
    </Root>
  )
}
