import { type SignupStatus, SIGNUP_STATUS_LABEL } from "@/lib/types";

const STYLE: Record<SignupStatus, string> = {
  PENDING: "bg-amber-50 text-amber-700 ring-amber-200",
  APPROVED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  REJECTED: "bg-red-50 text-red-700 ring-red-200",
};

export default function StatusBadge({ status }: { status: SignupStatus }) {
  return (
    <span
      className={`inline-block whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${STYLE[status]}`}
    >
      {SIGNUP_STATUS_LABEL[status]}
    </span>
  );
}
