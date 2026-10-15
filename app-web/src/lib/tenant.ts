export type Day = "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN";

export const DAY_LABELS: Record<Day, string> = {
  MON: "월",
  TUE: "화",
  WED: "수",
  THU: "목",
  FRI: "금",
  SAT: "토",
  SUN: "일",
};

export type BusinessHour = { day: Day; closed: boolean; open: string | null; close: string | null };

export type TenantProfile = {
  businessNo: string;
  industryCode: string;
  industryName: string;
  name: string;
  address: string | null;
  phone: string | null;
  businessHours: BusinessHour[];
  defaultDailyCapacity: number | null;
  orderLeadDays: number | null;
};
