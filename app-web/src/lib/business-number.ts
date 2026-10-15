/** 입력 중인 사업자등록번호에 하이픈을 넣는다: 1234567891 → 123-45-67891 */
export function formatBusinessNumber(input: string): string {
  const d = input.replace(/\D/g, "").slice(0, 10);
  if (d.length <= 3) return d;
  if (d.length <= 5) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 5)}-${d.slice(5)}`;
}
