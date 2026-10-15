import { Suspense } from "react";
import SignupRequestDetailView from "./detail-view";

export const metadata = { title: "회원가입 요청 상세 · 관리자" };

export default function SignupRequestDetailPage() {
  return (
    <Suspense>
      <SignupRequestDetailView />
    </Suspense>
  );
}
