"use client";

import { useEffect, useState } from "react";
import { FieldError, inputClass } from "@/components/auth-card";
import { useMe } from "@/components/app-shell";
import { api, ApiError } from "@/lib/api";
import { formatPhone } from "@/lib/signup";
import { DAY_LABELS, type BusinessHour, type TenantProfile } from "@/lib/tenant";

/** 설정 > 사업장 정보 (U-080) */
export default function BusinessSettings() {
  const [profile, setProfile] = useState<TenantProfile | null>(null);
  const [loadError, setLoadError] = useState("");
  const [saveCount, setSaveCount] = useState(0);

  useEffect(() => {
    api<TenantProfile>("/api/app/tenant")
      .then(setProfile)
      .catch((e) => setLoadError(e instanceof ApiError ? e.message : "사업장 정보를 불러오지 못했습니다."));
  }, []);

  return (
    <div className="max-w-3xl">
      <h1 className="text-lg font-semibold">사업장 정보</h1>
      <p className="mt-1 text-sm text-gray-500">영업시간과 생산 가능량은 주문 일정을 잡을 때 기준으로 씁니다.</p>
      {loadError && <p className="mt-6 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{loadError}</p>}
      {!profile && !loadError && <div className="mt-6 h-96 animate-pulse rounded-xl bg-gray-100" />}
      {/* 저장 후 서버 값으로 폼을 다시 시작한다 */}
      {profile && (
        <SettingsForm
          key={saveCount}
          initial={profile}
          justSaved={saveCount > 0}
          onSaved={(p) => {
            setProfile(p);
            setSaveCount((c) => c + 1);
          }}
        />
      )}
    </div>
  );
}

const labelClass = "mb-1.5 block text-sm font-medium text-gray-700";
const small = "rounded-lg border border-gray-300 px-2.5 py-1.5 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100 disabled:bg-gray-50 disabled:text-gray-300 aria-[invalid=true]:border-red-400";

function SettingsForm({
  initial,
  justSaved,
  onSaved,
}: {
  initial: TenantProfile;
  justSaved: boolean;
  onSaved: (p: TenantProfile) => void;
}) {
  const { reload } = useMe();
  const [name, setName] = useState(initial.name);
  const [address, setAddress] = useState(initial.address ?? "");
  const [phone, setPhone] = useState(initial.phone ?? "");
  const [hours, setHours] = useState<BusinessHour[]>(initial.businessHours);
  const [capacity, setCapacity] = useState(initial.defaultDailyCapacity?.toString() ?? "");
  const [leadDays, setLeadDays] = useState(initial.orderLeadDays?.toString() ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<{ ok: boolean; text: string } | null>(
    justSaved ? { ok: true, text: "저장했습니다." } : null,
  );
  const [saving, setSaving] = useState(false);

  function updateHour(index: number, patch: Partial<BusinessHour>) {
    setHours((prev) => prev.map((h, i) => (i === index ? { ...h, ...patch } : h)));
    setMessage(null);
  }

  function validate(): Record<string, string> {
    const v: Record<string, string> = {};
    if (!name.trim()) v.name = "상호를 입력해 주세요.";
    for (const h of hours) {
      if (h.closed) continue;
      if (!h.open || !h.close) v[`businessHours.${h.day}`] = "시간을 00:00 형식으로 입력해 주세요.";
      else if (h.open >= h.close) v[`businessHours.${h.day}`] = "마감 시간은 시작 시간보다 늦어야 합니다.";
    }
    if (capacity && !(Number(capacity) >= 1)) v.defaultDailyCapacity = "1 이상으로 입력해 주세요.";
    if (leadDays && !(Number(leadDays) >= 0 && Number(leadDays) <= 60)) v.orderLeadDays = "0~60일 사이로 입력해 주세요.";
    return v;
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setMessage(null);
    const v = validate();
    setErrors(v);
    if (Object.keys(v).length > 0) return;
    setSaving(true);
    try {
      const saved = await api<TenantProfile>("/api/app/tenant", {
        method: "PUT",
        body: JSON.stringify({
          name: name.trim(),
          address,
          phone,
          businessHours: hours.map((h) => (h.closed ? { day: h.day, closed: true } : h)),
          defaultDailyCapacity: capacity ? Number(capacity) : null,
          orderLeadDays: leadDays ? Number(leadDays) : null,
        }),
      });
      onSaved(saved);
      reload();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors);
        setMessage({ ok: false, text: err.message });
      } else {
        setMessage({ ok: false, text: "저장하지 못했습니다." });
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="mt-6 space-y-6">
      <section className="rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="mb-4 font-semibold">기본 정보</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <span className={labelClass}>사업자등록번호</span>
            <p className="py-2 text-[15px] text-gray-600">{initial.businessNo}</p>
          </div>
          <div>
            <span className={labelClass}>업종</span>
            <p className="py-2 text-[15px] text-gray-600">{initial.industryName}</p>
          </div>
          <div>
            <label htmlFor="name" className={labelClass}>상호</label>
            <input id="name" className={inputClass} value={name} maxLength={100} aria-invalid={!!errors.name}
              onChange={(e) => setName(e.target.value)} />
            <FieldError message={errors.name} />
          </div>
          <div>
            <label htmlFor="phone" className={labelClass}>연락처</label>
            <input id="phone" className={inputClass} value={phone} inputMode="tel" placeholder="02-123-4567"
              aria-invalid={!!errors.phone} onChange={(e) => setPhone(formatPhone(e.target.value))} />
            <FieldError message={errors.phone} />
          </div>
          <div className="sm:col-span-2">
            <label htmlFor="address" className={labelClass}>주소</label>
            <input id="address" className={inputClass} value={address} maxLength={200} placeholder="픽업 안내에 쓰는 매장 주소"
              aria-invalid={!!errors.address} onChange={(e) => setAddress(e.target.value)} />
            <FieldError message={errors.address} />
          </div>
        </div>
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="mb-1 font-semibold">영업시간</h2>
        <p className="mb-4 text-sm text-gray-500">쉬는 날은 픽업 날짜로 고를 수 없게 됩니다.</p>
        <div className="divide-y divide-gray-100">
          {hours.map((h, i) => {
            const err = errors[`businessHours.${h.day}`];
            return (
              <div key={h.day} className="py-2.5">
                <div className="flex flex-wrap items-center gap-3">
                  <span className="w-6 font-medium">{DAY_LABELS[h.day]}</span>
                  <label className="flex items-center gap-1.5 text-sm text-gray-600">
                    <input type="checkbox" checked={h.closed} className="h-4 w-4 accent-brand-500"
                      onChange={(e) => updateHour(i, e.target.checked
                        ? { closed: true }
                        : { closed: false, open: h.open ?? "10:00", close: h.close ?? "19:00" })} />
                    휴무
                  </label>
                  <input type="time" aria-label={`${DAY_LABELS[h.day]}요일 시작`} className={small} disabled={h.closed}
                    value={h.closed ? "" : (h.open ?? "")} aria-invalid={!!err}
                    onChange={(e) => updateHour(i, { open: e.target.value })} />
                  <span className="text-gray-400">~</span>
                  <input type="time" aria-label={`${DAY_LABELS[h.day]}요일 마감`} className={small} disabled={h.closed}
                    value={h.closed ? "" : (h.close ?? "")} aria-invalid={!!err}
                    onChange={(e) => updateHour(i, { close: e.target.value })} />
                </div>
                <FieldError message={err} />
              </div>
            );
          })}
        </div>
        <FieldError message={errors.businessHours} />
      </section>

      <section className="rounded-xl border border-gray-200 bg-white p-5">
        <h2 className="mb-4 font-semibold">주문 받는 기준</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="capacity" className={labelClass}>기본 하루 생산 가능량</label>
            <div className="flex items-center gap-2">
              <input id="capacity" type="number" min={1} className={inputClass} value={capacity}
                aria-invalid={!!errors.defaultDailyCapacity} onChange={(e) => setCapacity(e.target.value)} />
              <span className="shrink-0 text-sm text-gray-500">개</span>
            </div>
            <p className="mt-1 text-xs text-gray-500">품목별로 따로 정하지 않은 품목에 적용합니다.</p>
            <FieldError message={errors.defaultDailyCapacity} />
          </div>
          <div>
            <label htmlFor="leadDays" className={labelClass}>주문 마감</label>
            <div className="flex items-center gap-2">
              <span className="shrink-0 text-sm text-gray-500">픽업</span>
              <input id="leadDays" type="number" min={0} max={60} className={inputClass} value={leadDays}
                aria-invalid={!!errors.orderLeadDays} onChange={(e) => setLeadDays(e.target.value)} />
              <span className="shrink-0 text-sm text-gray-500">일 전까지</span>
            </div>
            <p className="mt-1 text-xs text-gray-500">0이면 당일 주문도 받습니다.</p>
            <FieldError message={errors.orderLeadDays} />
          </div>
        </div>
      </section>

      <div className="flex items-center justify-end gap-3">
        {message && (
          <p role="alert" className={`text-sm ${message.ok ? "text-green-700" : "text-red-600"}`}>{message.text}</p>
        )}
        <button type="submit" disabled={saving}
          className="rounded-lg bg-brand-500 px-5 py-2.5 text-[15px] font-semibold text-white transition hover:bg-brand-600 disabled:opacity-50">
          {saving ? "저장 중…" : "저장"}
        </button>
      </div>
    </form>
  );
}
