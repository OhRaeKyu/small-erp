"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import type { Me } from "@/lib/auth";
import { formatBusinessNumber } from "@/lib/business-number";

/** 승인 대기·반려는 오류가 아니라 안내로 보여준다 */
const NOTICE_CODES = new Set(["SIGNUP_PENDING", "SIGNUP_REJECTED"]);

function safeNext(next: string | null) {
  return next && next.startsWith("/") && !next.startsWith("//") ? next : "/";
}

export default function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<ApiError | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await api<Me>("/api/app/auth/login", {
        method: "POST",
        body: JSON.stringify({ loginId, password }),
      });
      router.replace(safeNext(searchParams.get("next")));
      router.refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err : new ApiError(0, "UNKNOWN", "로그인하지 못했습니다."));
      setSubmitting(false);
    }
  }

  const notice = error && NOTICE_CODES.has(error.code);

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
      <form onSubmit={onSubmit} className="space-y-4" noValidate>
        <div>
          <label htmlFor="loginId" className="mb-1.5 block text-sm font-medium text-gray-700">
            사업자등록번호
          </label>
          <input
            id="loginId"
            name="loginId"
            required
            autoFocus
            inputMode="numeric"
            autoComplete="username"
            placeholder="123-45-67890"
            value={loginId}
            onChange={(e) => setLoginId(formatBusinessNumber(e.target.value))}
            className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-[15px] outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
          />
        </div>
        <div>
          <label htmlFor="password" className="mb-1.5 block text-sm font-medium text-gray-700">
            비밀번호
          </label>
          <input
            id="password"
            name="password"
            type="password"
            required
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-[15px] outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
          />
        </div>

        {error && (
          <p
            role="alert"
            className={`rounded-lg px-3 py-2.5 text-sm ${
              notice ? "bg-amber-50 text-amber-800" : "bg-red-50 text-red-700"
            }`}
          >
            {error.message}
          </p>
        )}

        <button
          type="submit"
          disabled={submitting || !loginId || !password}
          className="w-full rounded-lg bg-brand-500 py-2.5 text-[15px] font-semibold text-white transition hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50"
        >
          {submitting ? "로그인 중…" : "로그인"}
        </button>
      </form>

      <div className="mt-6 flex items-center justify-center gap-3 border-t border-gray-100 pt-5 text-sm text-gray-500">
        <Link href="/signup" className="font-medium text-brand-600 hover:underline">
          회원가입
        </Link>
        <span className="text-gray-300">|</span>
        <Link href="/signup/status" className="hover:text-gray-700 hover:underline">
          가입 상태 조회
        </Link>
      </div>
    </div>
  );
}
