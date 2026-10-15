"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Me } from "@/lib/auth";

// 메뉴 구조는 planning/02-menu-screens.md. 각 메뉴는 해당 단계에서 열린다.
const MENUS = ["홈", "주문·판매", "생산", "재고", "구매", "품목·레시피", "문의 CS", "설정"];

export default function HomeView() {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);

  useEffect(() => {
    api<Me>("/api/app/auth/me")
      .then(setMe)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
      });
  }, [router]);

  async function logout() {
    await api("/api/app/auth/logout", { method: "POST" }).catch(() => undefined);
    router.replace("/login");
    router.refresh();
  }

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-56 shrink-0 border-r border-gray-200 bg-white md:block">
        <div className="flex h-14 items-center gap-2 border-b border-gray-100 px-5 font-semibold">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-500 text-sm text-white">E</span>
          나의 작은 ERP
        </div>
        <nav className="space-y-0.5 p-3 text-sm">
          {MENUS.map((m, i) => (
            <div
              key={m}
              className={`flex items-center justify-between rounded-lg px-3 py-2 ${
                i === 0 ? "bg-brand-50 font-medium text-brand-700" : "text-gray-400"
              }`}
            >
              {m}
              {i > 0 && <span className="text-[11px]">준비 중</span>}
            </div>
          ))}
        </nav>
      </aside>

      <div className="flex flex-1 flex-col">
        <header className="flex h-14 items-center justify-between border-b border-gray-200 bg-white px-6">
          <div className="text-sm">
            {me ? (
              <>
                <span className="font-semibold">{me.tenant.name}</span>
                <span className="ml-2 text-gray-400">{me.tenant.businessNo}</span>
              </>
            ) : (
              <span className="inline-block h-4 w-40 animate-pulse rounded bg-gray-100" />
            )}
          </div>
          <div className="flex items-center gap-3 text-sm">
            {me && <span className="text-gray-600">{me.user.name}</span>}
            <button onClick={logout} className="rounded-md border border-gray-200 px-3 py-1.5 text-gray-600 hover:bg-gray-50">
              로그아웃
            </button>
          </div>
        </header>
        <main className="flex-1 p-6">
          <h1 className="text-lg font-semibold">홈</h1>
          <div className="mt-4 rounded-xl border border-dashed border-gray-300 bg-white p-10 text-center text-sm text-gray-500">
            오늘 픽업 주문, 생산할 것, 발주 필요 품목이 여기에 보입니다. (주문·생산 단계에서 추가)
          </div>
        </main>
      </div>
    </div>
  );
}
