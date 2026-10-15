"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { inputClass, primaryButton } from "@/components/auth-card";
import { api, ApiError } from "@/lib/api";
import { formatBusinessNumber } from "@/lib/business-number";
import { PREFILL_KEY, type SignupStatusResult } from "@/lib/signup";

const STATUS_VIEW = {
  PENDING: { label: "승인 대기", tone: "bg-amber-50 text-amber-800", text: "관리자가 확인하고 있어요. 승인되면 메일로 알려드려요." },
  APPROVED: { label: "승인 완료", tone: "bg-emerald-50 text-emerald-800", text: "승인되었어요. 사업자등록번호로 로그인할 수 있어요." },
  REJECTED: { label: "반려", tone: "bg-red-50 text-red-800", text: "회원가입 요청이 반려되었어요. 사유를 확인하고 다시 요청해 주세요." },
} as const;

const dateFormat = new Intl.DateTimeFormat("ko-KR", { dateStyle: "medium", timeStyle: "short", timeZone: "Asia/Seoul" });

export default function StatusLookup() {
  const router = useRouter();
  const [businessNo, setBusinessNo] = useState("");
  const [password, setPassword] = useState("");
  const [result, setResult] = useState<SignupStatusResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      setResult(
        await api<SignupStatusResult>("/api/public/signup-requests/status", {
          method: "POST",
          body: JSON.stringify({ businessNo, password }),
        }),
      );
    } catch (err) {
      setResult(null);
      setError(err instanceof ApiError ? err.message : "조회하지 못했습니다.");
    } finally {
      setLoading(false);
    }
  }

  function reapply(r: SignupStatusResult) {
    try {
      sessionStorage.setItem(
        PREFILL_KEY,
        JSON.stringify({
          businessNo: r.businessNo,
          companyName: r.companyName,
          ownerName: r.ownerName,
          industryCode: r.industryCode,
          phone: r.phone,
          email: r.email,
        }),
      );
    } catch {
      // 저장소를 못 쓰면 빈 양식으로 연다
    }
    router.push("/signup");
  }

  if (result) {
    const view = STATUS_VIEW[result.status];
    return (
      <div className="space-y-4 text-sm">
        <div className={`rounded-lg p-4 ${view.tone}`}>
          <div className="text-base font-semibold">{view.label}</div>
          <p className="mt-1">{view.text}</p>
        </div>
        <dl className="grid grid-cols-[6rem_1fr] gap-y-2">
          <dt className="text-gray-500">상호</dt>
          <dd>{result.companyName}</dd>
          <dt className="text-gray-500">사업자번호</dt>
          <dd className="font-mono">{result.businessNo}</dd>
          <dt className="text-gray-500">요청일시</dt>
          <dd>{dateFormat.format(new Date(result.createdAt))}</dd>
          {result.reviewedAt && (
            <>
              <dt className="text-gray-500">처리일시</dt>
              <dd>{dateFormat.format(new Date(result.reviewedAt))}</dd>
            </>
          )}
          {result.rejectReason && (
            <>
              <dt className="text-gray-500">반려 사유</dt>
              <dd className="font-medium text-red-700">{result.rejectReason}</dd>
            </>
          )}
        </dl>
        {result.status === "REJECTED" && (
          <button onClick={() => reapply(result)} className={primaryButton}>
            내용 고쳐서 다시 회원가입
          </button>
        )}
        {result.status === "APPROVED" && (
          <Link href="/login" className={`${primaryButton} block text-center`}>
            로그인하기
          </Link>
        )}
        <button onClick={() => setResult(null)} className="w-full py-1 text-gray-500 hover:text-gray-800">
          다른 번호 조회
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-4">
      <div>
        <label htmlFor="businessNo" className="mb-1.5 block text-sm font-medium text-gray-700">
          사업자등록번호
        </label>
        <input
          id="businessNo"
          inputMode="numeric"
          placeholder="123-45-67890"
          value={businessNo}
          onChange={(e) => setBusinessNo(formatBusinessNumber(e.target.value))}
          className={inputClass}
        />
      </div>
      <div>
        <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-gray-700">
          비밀번호
        </label>
        <input
          id="password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className={inputClass}
        />
      </div>
      {error && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700">
          {error}
        </p>
      )}
      <button type="submit" disabled={loading || !businessNo || !password} className={primaryButton}>
        {loading ? "조회 중…" : "조회"}
      </button>
      <p className="text-center text-sm text-gray-500">
        <Link href="/login" className="hover:underline">
          로그인으로 돌아가기
        </Link>
      </p>
    </form>
  );
}
