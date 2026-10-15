import { Suspense } from "react";
import LoginForm from "./login-form";

export const metadata = { title: "관리자 로그인 · 나의 작은 ERP" };

export default function AdminLoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center bg-slate-900 px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center text-white">
          <div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-brand-500 text-lg font-bold">
            A
          </div>
          <h1 className="text-xl font-semibold">관리자 콘솔</h1>
          <p className="mt-1 text-sm text-slate-400">플랫폼 운영자 전용입니다</p>
        </div>
        <Suspense>
          <LoginForm />
        </Suspense>
      </div>
    </main>
  );
}
