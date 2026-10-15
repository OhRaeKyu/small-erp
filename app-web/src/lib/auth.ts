export type Role = "OWNER" | "MANAGER" | "STAFF";

export type Me = {
  user: { id: number; name: string; email: string };
  tenant: { id: number; name: string; businessNo: string; industryCode: string };
  role: Role;
};

