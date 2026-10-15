import { Suspense } from "react";
import LoginForm from "./login-form";

export const metadata = { title: "로그인 · 나의 작은 ERP" };

export default function LoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-brand-500 text-lg font-bold text-white">
            E
          </div>
          <h1 className="text-xl font-semibold">나의 작은 ERP</h1>
          <p className="mt-1 text-sm text-gray-500">주문부터 생산, 원재료 재고까지 한 곳에서</p>
        </div>
        <Suspense>
          <LoginForm />
        </Suspense>
      </div>
    </main>
  );
}
