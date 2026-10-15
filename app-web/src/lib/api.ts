export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

const AUTH_PATH = "/api/app/auth/";

// 동시에 여러 요청이 401을 받아도 갱신은 한 번만 한다
let refreshing: Promise<boolean> | null = null;

function refreshToken(): Promise<boolean> {
  refreshing ??= fetch(`${AUTH_PATH}refresh`, { method: "POST", credentials: "same-origin" })
    .then((res) => res.ok)
    .catch(() => false)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

async function send(path: string, init: RequestInit): Promise<Response> {
  try {
    return await fetch(path, {
      ...init,
      credentials: "same-origin",
      headers: { "Content-Type": "application/json", ...init.headers },
    });
  } catch {
    throw new ApiError(0, "NETWORK", "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }
}

/**
 * 같은 도메인의 /api 로 요청한다. 인증은 httpOnly 쿠키로 자동 전송된다.
 * 액세스 토큰이 만료되어 401이 오면 리프레시 쿠키로 한 번 갱신하고 다시 보낸다.
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  let res = await send(path, init);
  // 로그인·갱신·로그아웃 요청 자체는 갱신 대상이 아니다
  const isAuthCall = path.startsWith(AUTH_PATH) && !path.startsWith(`${AUTH_PATH}me`);
  if (res.status === 401 && !isAuthCall) {
    if (await refreshToken()) res = await send(path, init);
  }
  if (res.status === 204) return undefined as T;
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(
      res.status,
      body?.code ?? "UNKNOWN",
      body?.message ?? "요청을 처리하지 못했습니다.",
      body?.fieldErrors ?? {},
    );
  }
  return body as T;
}
