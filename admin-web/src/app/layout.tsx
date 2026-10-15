import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "관리자 · 나의 작은 ERP",
  description: "나의 작은 ERP 플랫폼 운영 콘솔",
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col font-sans">{children}</body>
    </html>
  );
}
