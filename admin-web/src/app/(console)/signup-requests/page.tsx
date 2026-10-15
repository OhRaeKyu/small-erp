import { Suspense } from "react";
import SignupRequestList from "./signup-request-list";

export const metadata = { title: "회원가입 요청 목록 · 관리자" };

export default function SignupRequestsPage() {
  return (
    <Suspense>
      <SignupRequestList />
    </Suspense>
  );
}
