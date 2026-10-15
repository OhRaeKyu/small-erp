import AuthCard from "@/components/auth-card";
import SignupForm from "./signup-form";

export const metadata = { title: "회원가입 · 나의 작은 ERP" };

export default function SignupPage() {
  return (
    <AuthCard title="회원가입" subtitle="관리자가 확인한 뒤 승인되면 바로 쓸 수 있어요" width="max-w-lg">
      <SignupForm />
    </AuthCard>
  );
}
