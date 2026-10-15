import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/** 관리자 쿠키가 없으면 로그인 화면으로 보낸다. 토큰 검증은 백엔드가 한다. */
export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl;
  if (pathname === "/login") {
    return NextResponse.next();
  }
  if (!request.cookies.has("ADMIN_TOKEN")) {
    const url = new URL("/login", request.url);
    url.searchParams.set("next", pathname + search);
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico).*)"],
};
