import Link from "next/link";

export const metadata = { title: "회원가입 · 나의 작은 ERP" };

// 가입 신청(U-002)·신청 상태 조회(U-003) 화면은 다음 작업에서 만든다. API는 준비되어 있다.
export default function SignupPlaceholder() {
  return (
    <main className="flex flex-1 items-center justify-center px-4">
      <div className="max-w-sm text-center">
        <h1 className="text-lg font-semibold">회원가입 화면을 준비하고 있어요</h1>
        <p className="mt-2 text-sm text-gray-500">곧 사업자등록번호로 회원가입할 수 있습니다.</p>
        <Link href="/login" className="mt-6 inline-block text-sm font-medium text-brand-600 hover:underline">
          로그인으로 돌아가기
        </Link>
      </div>
    </main>
  );
}
