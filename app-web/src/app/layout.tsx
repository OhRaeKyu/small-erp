import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "나의 작은 ERP",
  description: "직접 만드는 사업장을 위한 주문·생산·재고 관리",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className="h-full antialiased">
      <body className="min-h-full flex flex-col font-sans">{children}</body>
    </html>
  );
}
