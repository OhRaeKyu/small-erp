import AuthCard from "@/components/auth-card";
import StatusLookup from "./status-lookup";

export const metadata = { title: "가입 상태 조회 · 나의 작은 ERP" };

export default function SignupStatusPage() {
  return (
    <AuthCard title="가입 상태 조회" subtitle="회원가입할 때 입력한 사업자등록번호와 비밀번호로 확인해요">
      <StatusLookup />
    </AuthCard>
  );
}
