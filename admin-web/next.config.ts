import type { NextConfig } from "next";

// rewrites는 빌드 시점에 고정된다. Docker 빌드에서는 BACKEND_URL 빌드 인자로 넘긴다.
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Docker 이미지용 최소 실행 파일
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // 브라우저는 같은 도메인의 /api 로만 요청하고, Next 서버가 백엔드로 넘긴다 (httpOnly 쿠키 유지)
  async redirects() {
    return [{ source: "/", destination: "/signup-requests", permanent: false }];
  },
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
};

export default nextConfig;
