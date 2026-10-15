"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import StatusBadge from "@/components/status-badge";
import { api, ApiError } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import type { SignupRequestDetail } from "@/lib/types";

const AUDIT_LABEL = { APPROVE: "승인", REJECT: "반려" } as const;

export default function SignupRequestDetailView() {
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const [detail, setDetail] = useState<SignupRequestDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    api<SignupRequestDetail>(`/api/admin/signup-requests/${id}`)
      .then((d) => !cancelled && setDetail(d))
      .catch((e) => {
        if (cancelled) return;
        if (e instanceof ApiError && e.status === 401) return router.replace("/login");
        setLoadError(e instanceof ApiError && e.status === 404 ? "회원가입 요청을 찾을 수 없습니다." : "불러오지 못했습니다.");
      });
    return () => {
      cancelled = true;
    };
  }, [id, router]);

  if (loadError) {
    return (
      <div className="mx-auto max-w-5xl">
        <BackLink />
        <p className="mt-10 text-center text-sm text-gray-500">{loadError}</p>
      </div>
    );
  }
  if (!detail) {
    return (
      <div className="mx-auto max-w-5xl">
        <BackLink />
        <div className="mt-4 h-64 animate-pulse rounded-xl bg-white" />
      </div>
    );
  }

  const r = detail.request;
  return (
    <div className="mx-auto max-w-5xl">
      <BackLink />
      <div className="mt-3 mb-5 flex flex-wrap items-center gap-3">
        <h1 className="text-xl font-semibold">{r.companyName}</h1>
        <StatusBadge status={r.status} />
      </div>

      <div className="grid gap-5 lg:grid-cols-3">
        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm lg:col-span-2">
          <h2 className="mb-4 text-sm font-semibold text-gray-700">입력 정보</h2>
          <dl className="grid grid-cols-1 gap-x-6 gap-y-3 text-sm sm:grid-cols-[8rem_1fr]">
            <Field label="사업자등록번호" value={<span className="font-mono">{r.businessNo}</span>} />
            <Field label="상호" value={r.companyName} />
            <Field label="대표자" value={r.ownerName} />
            <Field label="업종" value={r.industryName} />
            <Field label="연락처" value={r.phone} />
            <Field label="이메일" value={r.email} />
            <Field
              label="사업자등록증"
              value={
                r.hasCertificate ? (
                  <a
                    href={`/api/admin/signup-requests/${r.id}/certificate`}
                    target="_blank"
                    rel="noreferrer"
                    className="font-medium text-brand-600 hover:underline"
                  >
                    첨부 파일 보기
                  </a>
                ) : (
                  <span className="text-gray-400">첨부 없음</span>
                )
              }
            />
            <Field label="요청일시" value={formatDateTime(r.createdAt)} />
          </dl>
        </section>

        <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-gray-700">처리</h2>
          {r.status === "PENDING" ? (
            <ReviewActions id={r.id} onDone={setDetail} />
          ) : (
            <dl className="space-y-3 text-sm">
              <Field label="결과" value={<StatusBadge status={r.status} />} />
              <Field label="처리자" value={detail.reviewerName ?? "–"} />
              <Field label="처리일시" value={formatDateTime(r.reviewedAt)} />
              {detail.rejectReason && <Field label="반려 사유" value={detail.rejectReason} />}
            </dl>
          )}
        </section>
      </div>

      <section className="mt-5 rounded-xl border border-gray-200 bg-white shadow-sm">
        <h2 className="border-b border-gray-100 px-5 py-3 text-sm font-semibold text-gray-700">
          같은 사업자등록번호의 다른 요청 <span className="font-normal text-gray-400">{detail.history.length}건</span>
        </h2>
        {detail.history.length === 0 ? (
          <p className="px-5 py-6 text-sm text-gray-400">이전 요청이 없습니다.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <tbody className="divide-y divide-gray-100">
              {detail.history.map((h) => (
                <tr key={h.id} className="hover:bg-gray-50">
                  <td className="px-5 py-2.5 tabular-nums text-gray-500">{formatDateTime(h.createdAt)}</td>
                  <td className="px-5 py-2.5">
                    <Link href={`/signup-requests/${h.id}`} className="hover:underline">
                      {h.companyName}
                    </Link>
                  </td>
                  <td className="px-5 py-2.5">
                    <StatusBadge status={h.status} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {detail.auditLogs.length > 0 && (
        <section className="mt-5 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
          <h2 className="mb-3 text-sm font-semibold text-gray-700">작업 이력</h2>
          <ul className="space-y-1.5 text-sm text-gray-600">
            {detail.auditLogs.map((a, i) => (
              <li key={i}>
                <span className="tabular-nums text-gray-400">{formatDateTime(a.createdAt)}</span> · {a.adminName ?? "알 수 없음"}{" "}
                님이 {AUDIT_LABEL[a.action]}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}

function BackLink() {
  return (
    <Link href="/signup-requests" className="text-sm text-gray-500 hover:text-gray-800">
      ← 회원가입 요청 목록
    </Link>
  );
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <>
      <dt className="text-gray-500">{label}</dt>
      <dd className="text-gray-900">{value}</dd>
    </>
  );
}

function ReviewActions({ id, onDone }: { id: number; onDone: (d: SignupRequestDetail) => void }) {
  const [mode, setMode] = useState<"idle" | "approve" | "reject">("idle");
  const [reason, setReason] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function submit(path: "approve" | "reject") {
    setBusy(true);
    setError(null);
    try {
      const d = await api<SignupRequestDetail>(`/api/admin/signup-requests/${id}/${path}`, {
        method: "POST",
        body: path === "reject" ? JSON.stringify({ reason }) : undefined,
      });
      onDone(d);
    } catch (e) {
      if (e instanceof ApiError) setError(e.fieldErrors.reason ?? e.message);
      else setError("처리하지 못했습니다.");
      setBusy(false);
    }
  }

  const primary =
    "w-full rounded-lg py-2 text-sm font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-50";

  return (
    <div className="space-y-3 text-sm">
      {mode === "idle" && (
        <>
          <p className="text-gray-500">입력 정보와 사업자등록증을 확인한 뒤 처리해 주세요.</p>
          <button onClick={() => setMode("approve")} className={`${primary} bg-brand-500 hover:bg-brand-600`}>
            승인
          </button>
          <button
            onClick={() => setMode("reject")}
            className="w-full rounded-lg border border-gray-300 py-2 font-semibold text-gray-700 hover:bg-gray-50"
          >
            반려
          </button>
        </>
      )}

      {mode === "approve" && (
        <>
          <p className="rounded-lg bg-brand-50 p-3 text-brand-700">
            승인하면 사업장과 계정이 만들어지고, 신청자는 사업자등록번호로 바로 로그인할 수 있어요. 안내 메일도 보냅니다.
          </p>
          <button disabled={busy} onClick={() => submit("approve")} className={`${primary} bg-brand-500 hover:bg-brand-600`}>
            {busy ? "처리 중…" : "승인 확정"}
          </button>
          <button disabled={busy} onClick={() => setMode("idle")} className="w-full py-1.5 text-gray-500 hover:text-gray-800">
            취소
          </button>
        </>
      )}

      {mode === "reject" && (
        <>
          <label htmlFor="reason" className="block font-medium text-gray-700">
            반려 사유 <span className="font-normal text-gray-400">(신청자에게 메일로 전달돼요)</span>
          </label>
          <textarea
            id="reason"
            rows={4}
            autoFocus
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="예: 사업자등록증의 상호와 입력한 상호가 다릅니다."
            className="w-full rounded-lg border border-gray-300 px-3 py-2 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
          />
          <p className="text-xs text-gray-400">5자 이상 입력해 주세요.</p>
          <button
            disabled={busy || reason.trim().length < 5}
            onClick={() => submit("reject")}
            className={`${primary} bg-red-600 hover:bg-red-700`}
          >
            {busy ? "처리 중…" : "반려 확정"}
          </button>
          <button disabled={busy} onClick={() => setMode("idle")} className="w-full py-1.5 text-gray-500 hover:text-gray-800">
            취소
          </button>
        </>
      )}

      {error && (
        <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
