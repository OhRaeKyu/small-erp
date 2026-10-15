"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { createContext, Suspense, useContext, useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Me } from "@/lib/auth";

// 메뉴 구조는 planning/02-menu-screens.md. href 없는 메뉴는 해당 단계에서 연다.
const NAV: { label: string; href?: string; children?: { label: string; href: string }[] }[] = [
  { label: "홈", href: "/" },
  { label: "주문·판매" },
  { label: "생산" },
  { label: "재고" },
  { label: "구매" },
  { label: "품목·레시피" },
  { label: "문의 CS" },
  { label: "설정", children: [{ label: "사업장 정보", href: "/settings/business" }] },
];

/** 상단 상호처럼 화면 여러 곳에서 쓰는 로그인 정보. 사업장 정보를 고치면 reload로 다시 읽는다 */
const MeContext = createContext<{ me: Me | null; reload: () => void }>({ me: null, reload: () => {} });

export function useMe() {
  return useContext(MeContext);
}

export default function AppShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    api<Me>("/api/app/auth/me")
      .then(setMe)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
      });
  }, [router, version]);

  async function logout() {
    await api("/api/app/auth/logout", { method: "POST" }).catch(() => undefined);
    router.replace("/login");
    router.refresh();
  }

  return (
    <MeContext value={{ me, reload: () => setVersion((v) => v + 1) }}>
      <div className="flex min-h-screen">
        <aside className="hidden w-56 shrink-0 border-r border-gray-200 bg-white md:block">
          <div className="flex h-14 items-center gap-2 border-b border-gray-100 px-5 font-semibold">
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-500 text-sm text-white">E</span>
            나의 작은 ERP
          </div>
          <Suspense fallback={<nav className="p-3" />}>
            <AppNav />
          </Suspense>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
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
          <main className="min-w-0 flex-1 p-6">{children}</main>
        </div>
      </div>
    </MeContext>
  );
}

/** 현재 경로를 읽는 부분만 Suspense 안에 둔다 */
function AppNav() {
  const pathname = usePathname();
  const active = (href: string) => (href === "/" ? pathname === "/" : pathname.startsWith(href));
  const linkClass = (href: string) =>
    `block rounded-lg px-3 py-2 ${active(href) ? "bg-brand-50 font-medium text-brand-700" : "text-gray-700 hover:bg-gray-50"}`;

  return (
    <nav className="space-y-0.5 p-3 text-sm">
      {NAV.map((item) =>
        item.href ? (
          <Link key={item.label} href={item.href} className={linkClass(item.href)}>
            {item.label}
          </Link>
        ) : item.children ? (
          <div key={item.label} className="pt-3">
            <div className="px-3 pb-1 text-[11px] font-semibold tracking-wide text-gray-400">{item.label}</div>
            {item.children.map((c) => (
              <Link key={c.href} href={c.href} className={linkClass(c.href)}>
                {c.label}
              </Link>
            ))}
          </div>
        ) : (
          <div key={item.label} className="flex items-center justify-between rounded-lg px-3 py-2 text-gray-400">
            {item.label}
            <span className="text-[11px]">준비 중</span>
          </div>
        ),
      )}
    </nav>
  );
}
