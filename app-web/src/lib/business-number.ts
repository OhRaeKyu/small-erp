/** 입력 중인 사업자등록번호에 하이픈을 넣는다: 1234567891 → 123-45-67891 */
export function formatBusinessNumber(input: string): string {
  const d = input.replace(/\D/g, "").slice(0, 10);
  if (d.length <= 3) return d;
  if (d.length <= 5) return `${d.slice(0, 3)}-${d.slice(3)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 5)}-${d.slice(5)}`;
}

const WEIGHTS = [1, 3, 7, 1, 3, 7, 1, 3, 5];

/** 국세청 검증번호(체크섬) 확인. 서버도 같은 규칙으로 다시 확인한다. */
export function isValidBusinessNumber(input: string): boolean {
  const d = input.replace(/-/g, "");
  if (!/^\d{10}$/.test(d)) return false;
  const n = [...d].map(Number);
  let sum = WEIGHTS.reduce((acc, w, i) => acc + n[i] * w, 0);
  sum += Math.floor((n[8] * 5) / 10);
  return (10 - (sum % 10)) % 10 === n[9];
}
