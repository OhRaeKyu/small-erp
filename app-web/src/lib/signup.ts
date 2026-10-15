export type SignupStatus = "PENDING" | "APPROVED" | "REJECTED";

export type IndustryOption = { code: string; name: string };

export type SignupStatusResult = {
  status: SignupStatus;
  businessNo: string;
  companyName: string;
  ownerName: string;
  industryCode: string;
  phone: string;
  email: string;
  rejectReason: string | null;
  createdAt: string;
  reviewedAt: string | null;
};

/** 반려 후 다시 회원가입할 때 이전 입력값을 넘겨주는 저장소 키 (같은 탭에서만 유지) */
export const PREFILL_KEY = "signup-prefill";

export function formatPhone(input: string): string {
  const d = input.replace(/\D/g, "").slice(0, 11);
  if (d.startsWith("02")) {
    if (d.length <= 2) return d;
    if (d.length <= 5) return `${d.slice(0, 2)}-${d.slice(2)}`;
    if (d.length <= 9) return `${d.slice(0, 2)}-${d.slice(2, 5)}-${d.slice(5)}`;
    return `${d.slice(0, 2)}-${d.slice(2, 6)}-${d.slice(6, 10)}`;
  }
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}-${d.slice(3)}`;
  if (d.length <= 10) return `${d.slice(0, 3)}-${d.slice(3, 6)}-${d.slice(6)}`;
  return `${d.slice(0, 3)}-${d.slice(3, 7)}-${d.slice(7)}`;
}
