"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { FieldError, inputClass, primaryButton } from "@/components/auth-card";
import { api, ApiError } from "@/lib/api";
import { formatBusinessNumber, isValidBusinessNumber } from "@/lib/business-number";
import { formatPhone, type IndustryOption, PREFILL_KEY } from "@/lib/signup";

type Form = {
  businessNo: string;
  companyName: string;
  ownerName: string;
  industryCode: string;
  phone: string;
  email: string;
  password: string;
  passwordConfirm: string;
};

const EMPTY: Form = {
  businessNo: "",
  companyName: "",
  ownerName: "",
  industryCode: "",
  phone: "",
  email: "",
  password: "",
  passwordConfirm: "",
};

const MAX_FILE = 5 * 1024 * 1024;
const ACCEPT = ".jpg,.jpeg,.png,.pdf";

function omit<T extends object>(obj: T, key: string): T {
  const copy = { ...obj } as Record<string, unknown>;
  delete copy[key];
  return copy as T;
}

function validate(f: Form, file: File | null): Record<string, string> {
  const e: Record<string, string> = {};
  if (!f.businessNo) e.businessNo = "사업자등록번호를 입력해 주세요.";
  else if (!isValidBusinessNumber(f.businessNo)) e.businessNo = "올바른 사업자등록번호가 아닙니다.";
  if (!f.companyName.trim()) e.companyName = "상호를 입력해 주세요.";
  if (!f.ownerName.trim()) e.ownerName = "대표자명을 입력해 주세요.";
  if (!f.industryCode) e.industryCode = "업종을 선택해 주세요.";
  if (!/^[0-9-]{9,13}$/.test(f.phone)) e.phone = "연락처를 확인해 주세요.";
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(f.email)) e.email = "이메일 형식이 아닙니다.";
  if (f.password.length < 8 || !/[A-Za-z]/.test(f.password) || !/\d/.test(f.password))
    e.password = "영문과 숫자를 섞어 8자 이상 입력해 주세요.";
  if (f.password !== f.passwordConfirm) e.passwordConfirm = "비밀번호가 서로 다릅니다.";
  if (file && file.size > MAX_FILE) e.certificate = "파일은 5MB 이하만 올릴 수 있습니다.";
  return e;
}

export default function SignupForm() {
  const [form, setForm] = useState<Form>(EMPTY);
  const [file, setFile] = useState<File | null>(null);
  const [industries, setIndustries] = useState<IndustryOption[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [isReapply, setIsReapply] = useState(false);

  useEffect(() => {
    api<IndustryOption[]>("/api/public/industry-packs").then(setIndustries).catch(() => setIndustries([]));
  }, []);

  // 화면에 들어올 때마다 새 양식으로 시작한다. Next가 이전 화면 상태를 보관했다가 다시 보여줄 수 있어서
  // (예: 요청 완료 후 다시 들어옴) 마운트·재표시 시점에 초기화하고,
  // 반려 후 "다시 회원가입"으로 들어왔으면 이전 입력값을 채운다 (브라우저 저장소라 렌더 후에 읽는다).
  useEffect(() => {
    let prev: Partial<Form> | null = null;
    try {
      const saved = sessionStorage.getItem(PREFILL_KEY);
      sessionStorage.removeItem(PREFILL_KEY);
      prev = saved ? (JSON.parse(saved) as Partial<Form>) : null;
    } catch {
      prev = null;
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect -- 외부 저장소(sessionStorage)와 동기화
    setForm({ ...EMPTY, ...prev, password: "", passwordConfirm: "" });
    setIsReapply(prev !== null);
    setDone(false);
    setErrors({});
    setFile(null);
  }, []);

  function set<K extends keyof Form>(key: K, value: Form[K]) {
    setForm((f) => ({ ...f, [key]: value }));
    setErrors((prev) => omit(prev, key));
  }

  async function onSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setFormError(null);
    const v = validate(form, file);
    setErrors(v);
    if (Object.keys(v).length > 0) return;

    const request = omit(form, "passwordConfirm");
    const body = new FormData();
    body.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }));
    if (file) body.append("certificate", file);

    setSubmitting(true);
    try {
      await api("/api/public/signup-requests", { method: "POST", body });
      setDone(true);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        if (err.code === "INVALID_BUSINESS_NO") setErrors({ businessNo: err.message });
        else if (["INVALID_FILE_TYPE", "FILE_TOO_LARGE"].includes(err.code)) setErrors({ certificate: err.message });
        else if (Object.keys(err.fieldErrors).length === 0) setFormError(err.message);
      } else setFormError("요청을 보내지 못했습니다.");
    } finally {
      setSubmitting(false);
    }
  }

  if (done) {
    return (
      <div className="py-4 text-center">
        <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-brand-50 text-2xl text-brand-600">
          ✓
        </div>
        <h2 className="text-lg font-semibold">회원가입 요청을 보냈어요</h2>
        <p className="mt-2 text-sm leading-6 text-gray-500">
          관리자가 확인한 뒤 승인되면 {form.email} 로 알려드려요.
          <br />
          진행 상황은 사업자등록번호와 비밀번호로 조회할 수 있어요.
        </p>
        <div className="mt-6 flex justify-center gap-3 text-sm">
          <Link href="/signup/status" className="rounded-lg border border-gray-300 px-4 py-2 font-medium hover:bg-gray-50">
            가입 상태 조회
          </Link>
          <Link href="/login" className="rounded-lg bg-brand-500 px-4 py-2 font-medium text-white hover:bg-brand-600">
            로그인 화면으로
          </Link>
        </div>
      </div>
    );
  }

  const field = (key: keyof Form | "certificate") => ({ "aria-invalid": Boolean(errors[key]) });

  return (
    <form onSubmit={onSubmit} noValidate className="space-y-4">
      {isReapply && (
        <p className="rounded-lg bg-amber-50 px-3 py-2.5 text-sm text-amber-800">
          이전에 입력한 내용을 채워 두었어요. 반려 사유를 고쳐 다시 보내 주세요.
        </p>
      )}
      <div>
        <Label htmlFor="businessNo">사업자등록번호</Label>
        <input
          id="businessNo"
          inputMode="numeric"
          placeholder="123-45-67890"
          value={form.businessNo}
          onChange={(e) => set("businessNo", formatBusinessNumber(e.target.value))}
          className={inputClass}
          {...field("businessNo")}
        />
        <FieldError message={errors.businessNo} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="companyName">상호</Label>
          <input
            id="companyName"
            value={form.companyName}
            onChange={(e) => set("companyName", e.target.value)}
            className={inputClass}
            {...field("companyName")}
          />
          <FieldError message={errors.companyName} />
        </div>
        <div>
          <Label htmlFor="ownerName">대표자명</Label>
          <input
            id="ownerName"
            value={form.ownerName}
            onChange={(e) => set("ownerName", e.target.value)}
            className={inputClass}
            {...field("ownerName")}
          />
          <FieldError message={errors.ownerName} />
        </div>
      </div>
      <div>
        <Label htmlFor="industryCode">업종</Label>
        <select
          id="industryCode"
          value={form.industryCode}
          onChange={(e) => set("industryCode", e.target.value)}
          className={`${inputClass} bg-white`}
          {...field("industryCode")}
        >
          <option value="">선택해 주세요</option>
          {industries.map((i) => (
            <option key={i.code} value={i.code}>
              {i.name}
            </option>
          ))}
        </select>
        <FieldError message={errors.industryCode} />
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="phone">연락처</Label>
          <input
            id="phone"
            inputMode="tel"
            placeholder="010-1234-5678"
            value={form.phone}
            onChange={(e) => set("phone", formatPhone(e.target.value))}
            className={inputClass}
            {...field("phone")}
          />
          <FieldError message={errors.phone} />
        </div>
        <div>
          <Label htmlFor="email">이메일</Label>
          <input
            id="email"
            type="email"
            placeholder="승인 안내를 받을 주소"
            value={form.email}
            onChange={(e) => set("email", e.target.value)}
            className={inputClass}
            {...field("email")}
          />
          <FieldError message={errors.email} />
        </div>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Label htmlFor="password">비밀번호</Label>
          <input
            id="password"
            type="password"
            autoComplete="new-password"
            placeholder="영문+숫자 8자 이상"
            value={form.password}
            onChange={(e) => set("password", e.target.value)}
            className={inputClass}
            {...field("password")}
          />
          <FieldError message={errors.password} />
        </div>
        <div>
          <Label htmlFor="passwordConfirm">비밀번호 확인</Label>
          <input
            id="passwordConfirm"
            type="password"
            autoComplete="new-password"
            value={form.passwordConfirm}
            onChange={(e) => set("passwordConfirm", e.target.value)}
            className={inputClass}
            {...field("passwordConfirm")}
          />
          <FieldError message={errors.passwordConfirm} />
        </div>
      </div>
      <div>
        <Label htmlFor="certificate">
          사업자등록증 <span className="font-normal text-gray-400">(선택 · jpg, png, pdf · 5MB 이하)</span>
        </Label>
        <input
          id="certificate"
          type="file"
          accept={ACCEPT}
          onChange={(e) => {
            setFile(e.target.files?.[0] ?? null);
            setErrors((prev) => omit(prev, "certificate"));
          }}
          className="block w-full text-sm text-gray-600 file:mr-3 file:rounded-md file:border-0 file:bg-gray-100 file:px-3 file:py-2 file:text-sm file:font-medium hover:file:bg-gray-200"
          {...field("certificate")}
        />
        <p className="mt-1 text-xs text-gray-400">첨부하면 승인이 빨라져요. 관리자만 볼 수 있어요.</p>
        <FieldError message={errors.certificate} />
      </div>

      {formError && (
        <p role="alert" className="rounded-lg bg-red-50 px-3 py-2.5 text-sm text-red-700">
          {formError}
        </p>
      )}

      <button type="submit" disabled={submitting} className={primaryButton}>
        {submitting ? "보내는 중…" : "회원가입 요청"}
      </button>
      <p className="text-center text-sm text-gray-500">
        이미 계정이 있나요?{" "}
        <Link href="/login" className="font-medium text-brand-600 hover:underline">
          로그인
        </Link>
      </p>
    </form>
  );
}

function Label({ htmlFor, children }: { htmlFor: string; children: React.ReactNode }) {
  return (
    <label htmlFor={htmlFor} className="mb-1.5 block text-sm font-medium text-gray-700">
      {children}
    </label>
  );
}
