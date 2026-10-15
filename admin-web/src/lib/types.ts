export type AdminMe = { id: number; email: string; name: string; role: "SUPER" | "OPERATOR" };

export type SignupStatus = "PENDING" | "APPROVED" | "REJECTED";

export const SIGNUP_STATUS_LABEL: Record<SignupStatus, string> = {
  PENDING: "승인 대기",
  APPROVED: "승인",
  REJECTED: "반려",
};

export type SignupRequestRow = {
  id: number;
  businessNo: string;
  companyName: string;
  ownerName: string;
  industryCode: string;
  industryName: string;
  phone: string;
  email: string;
  status: SignupStatus;
  hasCertificate: boolean;
  createdAt: string;
  reviewedAt: string | null;
};

export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};
