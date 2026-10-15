package com.smallerp.account;

/** 요일별 영업시간. 쉬는 날이면 open/close는 비워 둔다. 시간은 "HH:mm". */
public record BusinessHour(DayOfWeekCode day, boolean closed, String open, String close) {

    public enum DayOfWeekCode { MON, TUE, WED, THU, FRI, SAT, SUN }
}
