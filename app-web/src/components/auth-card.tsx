import Link from "next/link";

/** 로그인·회원가입 화면 공통 틀 */
export default function AuthCard({
  title,
  subtitle,
  width = "max-w-sm",
  children,
}: {
  title: string;
  subtitle?: string;
  width?: string;
  children: React.ReactNode;
}) {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-12">
      <div className={`w-full ${width}`}>
        <div className="mb-8 text-center">
          <Link
            href="/login"
            className="mx-auto mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-brand-500 text-lg font-bold text-white"
          >
            E
          </Link>
          <h1 className="text-xl font-semibold">{title}</h1>
          {subtitle && <p className="mt-1 text-sm text-gray-500">{subtitle}</p>}
        </div>
        <div className="rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">{children}</div>
      </div>
    </main>
  );
}

export const inputClass =
  "w-full rounded-lg border border-gray-300 px-3 py-2.5 text-[15px] outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100 aria-[invalid=true]:border-red-400";

export const primaryButton =
  "w-full rounded-lg bg-brand-500 py-2.5 text-[15px] font-semibold text-white transition hover:bg-brand-600 disabled:cursor-not-allowed disabled:opacity-50";

export function FieldError({ message }: { message?: string }) {
  return message ? <p className="mt-1 text-xs text-red-600">{message}</p> : null;
}
