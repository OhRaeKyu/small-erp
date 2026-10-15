"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { type Page, type SignupRequestRow, type SignupStatus, SIGNUP_STATUS_LABEL } from "@/lib/types";

const TABS: { value: SignupStatus | ""; label: string }[] = [
  { value: "PENDING", label: "승인 대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "반려" },
  { value: "", label: "전체" },
];

const STATUS_BADGE: Record<SignupStatus, string> = {
  PENDING: "bg-amber-50 text-amber-700 ring-amber-200",
  APPROVED: "bg-emerald-50 text-emerald-700 ring-emerald-200",
  REJECTED: "bg-red-50 text-red-700 ring-red-200",
};

const PAGE_SIZE = 20;

const dateFormat = new Intl.DateTimeFormat("ko-KR", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
  timeZone: "Asia/Seoul",
});

type Counts = Record<SignupStatus, number>;

export default function SignupRequestList() {
  const router = useRouter();
  const pathname = usePathname();
  const params = useSearchParams();

  // 필터는 URL에 둔다: 새로고침·뒤로가기·링크 공유 시 그대로 유지
  const status = (params.get("status") ?? "PENDING") as SignupStatus | "";
  const q = params.get("q") ?? "";
  // 기본: 먼저 온 신청부터 처리하도록 오래된 순
  const sort = params.get("sort") === "LATEST" ? "LATEST" : "OLDEST";
  const page = Math.max(Number(params.get("page") ?? 0) || 0, 0);

  const queryKey = (() => {
    const query = new URLSearchParams({ sort, page: String(page), size: String(PAGE_SIZE) });
    if (status) query.set("status", status);
    if (q) query.set("q", q);
    return query.toString();
  })();

  // 응답을 요청한 쿼리와 함께 보관해, 쿼리가 바뀌면 바로 "불러오는 중"으로 본다
  const [result, setResult] = useState<{
    key: string;
    data?: Page<SignupRequestRow>;
    counts?: Counts;
    error?: string;
  } | null>(null);
  const loading = result?.key !== queryKey;
  const data = result?.data ?? null;
  const counts = result?.counts ?? null;
  const error = loading ? null : (result?.error ?? null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      api<Page<SignupRequestRow>>(`/api/admin/signup-requests?${queryKey}`),
      api<Counts>("/api/admin/signup-requests/counts"),
    ])
      .then(([list, c]) => {
        if (!cancelled) setResult({ key: queryKey, data: list, counts: c });
      })
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) {
          router.replace("/login");
          return;
        }
        setResult((prev) => ({
          ...prev,
          key: queryKey,
          error: e instanceof ApiError ? e.message : "목록을 불러오지 못했습니다.",
        }));
      });
    return () => {
      cancelled = true;
    };
  }, [queryKey, router]);

  function update(next: Record<string, string>) {
    const sp = new URLSearchParams(params);
    for (const [k, v] of Object.entries(next)) sp.set(k, v);
    if (!("page" in next)) sp.delete("page");
    router.push(`${pathname}?${sp}`);
  }

  const total = counts ? counts.PENDING + counts.APPROVED + counts.REJECTED : null;
  const countOf = (v: SignupStatus | "") => (v ? counts?.[v] : total);

  return (
    <div className="mx-auto max-w-7xl">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold">회원가입 요청 목록</h1>
          <p className="mt-1 text-sm text-gray-500">사업자가 보낸 회원가입 요청을 확인하고 승인·반려합니다.</p>
        </div>
      </div>

      <div className="rounded-xl border border-gray-200 bg-white shadow-sm">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-100 px-4 pt-3">
          <div role="tablist" className="flex gap-1 text-sm">
            {TABS.map((t) => {
              const active = status === t.value;
              return (
                <button
                  key={t.label}
                  role="tab"
                  aria-selected={active}
                  onClick={() => update({ status: t.value })}
                  className={`-mb-px border-b-2 px-3 pb-2.5 pt-1 font-medium transition ${
                    active ? "border-brand-500 text-brand-600" : "border-transparent text-gray-500 hover:text-gray-800"
                  }`}
                >
                  {t.label}
                  <span
                    className={`ml-1.5 rounded-full px-1.5 py-0.5 text-xs ${
                      active ? "bg-brand-50 text-brand-600" : "bg-gray-100 text-gray-500"
                    }`}
                  >
                    {countOf(t.value) ?? "–"}
                  </span>
                </button>
              );
            })}
          </div>
          <div className="flex items-center gap-2 pb-3">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const value = new FormData(e.currentTarget).get("q");
                update({ q: String(value ?? "").trim() });
              }}
            >
              <input
                key={q}
                name="q"
                type="search"
                defaultValue={q}
                placeholder="사업자번호·상호·대표자 검색"
                aria-label="검색"
                className="w-60 rounded-lg border border-gray-300 px-3 py-1.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
              />
            </form>
            <select
              value={sort}
              onChange={(e) => update({ sort: e.target.value })}
              aria-label="정렬"
              className="rounded-lg border border-gray-300 px-2 py-1.5 text-sm outline-none focus:border-brand-500"
            >
              <option value="OLDEST">오래된 순</option>
              <option value="LATEST">최근 순</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[960px] text-left text-sm">
            <thead className="bg-gray-50 text-xs text-gray-500">
              <tr>
                <th className="px-4 py-2.5 font-medium">요청일시</th>
                <th className="px-4 py-2.5 font-medium">사업자등록번호</th>
                <th className="px-4 py-2.5 font-medium">상호</th>
                <th className="px-4 py-2.5 font-medium">대표자</th>
                <th className="px-4 py-2.5 font-medium">업종</th>
                <th className="px-4 py-2.5 font-medium">연락처</th>
                <th className="px-4 py-2.5 font-medium">이메일</th>
                <th className="px-4 py-2.5 font-medium">등록증</th>
                <th className="px-4 py-2.5 font-medium">상태</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading && !data
                ? Array.from({ length: 5 }, (_, i) => (
                    <tr key={i}>
                      <td colSpan={9} className="px-4 py-3">
                        <div className="h-4 animate-pulse rounded bg-gray-100" />
                      </td>
                    </tr>
                  ))
                : data?.content.map((r) => (
                    <tr key={r.id} className="hover:bg-gray-50">
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-500">
                        {dateFormat.format(new Date(r.createdAt))}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3 font-mono tabular-nums">{r.businessNo}</td>
                      <td className="px-4 py-3 font-medium">{r.companyName}</td>
                      <td className="whitespace-nowrap px-4 py-3">{r.ownerName}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-gray-600">{r.industryName}</td>
                      <td className="whitespace-nowrap px-4 py-3 tabular-nums text-gray-600">{r.phone}</td>
                      <td className="px-4 py-3 text-gray-600">{r.email}</td>
                      <td className="px-4 py-3 text-gray-500">{r.hasCertificate ? "첨부" : "–"}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`inline-block whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${STATUS_BADGE[r.status]}`}
                        >
                          {SIGNUP_STATUS_LABEL[r.status]}
                        </span>
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
          {error && <p className="px-4 py-10 text-center text-sm text-red-600">{error}</p>}
          {!error && data && data.content.length === 0 && (
            <p className="px-4 py-14 text-center text-sm text-gray-500">
              {q ? `"${q}"에 해당하는 요청이 없습니다.` : "해당 상태의 회원가입 요청이 없습니다."}
            </p>
          )}
        </div>

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-gray-100 px-4 py-3 text-sm text-gray-600">
            <span>
              총 {data.totalElements}건 · {data.page + 1} / {data.totalPages} 페이지
            </span>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => update({ page: String(page - 1) })}
                className="rounded-md border border-gray-200 px-3 py-1 disabled:opacity-40"
              >
                이전
              </button>
              <button
                disabled={page + 1 >= data.totalPages}
                onClick={() => update({ page: String(page + 1) })}
                className="rounded-md border border-gray-200 px-3 py-1 disabled:opacity-40"
              >
                다음
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
