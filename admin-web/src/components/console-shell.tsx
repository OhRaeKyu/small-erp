"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { AdminMe } from "@/lib/types";

// 메뉴 구조는 planning/02-menu-screens.md 2장. href 없는 메뉴는 다음 단계에서 연다.
const NAV: { group: string; items: { label: string; href?: string }[] }[] = [
  { group: "회원가입", items: [{ label: "요청 목록", href: "/signup-requests" }] },
  { group: "사업장", items: [{ label: "사업장 목록" }] },
  { group: "운영", items: [{ label: "업종 팩" }, { label: "작업 이력" }] },
];

export default function ConsoleShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [me, setMe] = useState<AdminMe | null>(null);

  useEffect(() => {
    api<AdminMe>("/api/admin/auth/me")
      .then(setMe)
      .catch((e) => {
        if (e instanceof ApiError && e.status === 401) router.replace("/login");
      });
  }, [router]);

  async function logout() {
    await api("/api/admin/auth/logout", { method: "POST" }).catch(() => undefined);
    router.replace("/login");
    router.refresh();
  }

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-56 shrink-0 bg-slate-900 text-slate-300 md:block">
        <div className="flex h-14 items-center gap-2 px-5 font-semibold text-white">
          <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-500 text-sm">A</span>
          관리자 콘솔
        </div>
        <nav className="space-y-5 px-3 py-4 text-sm">
          {NAV.map((section) => (
            <div key={section.group}>
              <div className="px-3 pb-1.5 text-[11px] font-semibold tracking-wide text-slate-500">{section.group}</div>
              {section.items.map((item) =>
                item.href ? (
                  <Link
                    key={item.label}
                    href={item.href}
                    className={`block rounded-lg px-3 py-2 ${
                      pathname.startsWith(item.href) ? "bg-slate-800 font-medium text-white" : "hover:bg-slate-800/60"
                    }`}
                  >
                    {item.label}
                  </Link>
                ) : (
                  <div key={item.label} className="flex justify-between px-3 py-2 text-slate-500">
                    {item.label}
                    <span className="text-[11px]">준비 중</span>
                  </div>
                ),
              )}
            </div>
          ))}
        </nav>
      </aside>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-end gap-3 border-b border-gray-200 bg-white px-6 text-sm">
          {me ? (
            <span className="text-gray-600">
              {me.name} <span className="text-gray-400">· {me.email}</span>
            </span>
          ) : (
            <span className="inline-block h-4 w-40 animate-pulse rounded bg-gray-100" />
          )}
          <button onClick={logout} className="rounded-md border border-gray-200 px-3 py-1.5 text-gray-600 hover:bg-gray-50">
            로그아웃
          </button>
        </header>
        <main className="min-w-0 flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
