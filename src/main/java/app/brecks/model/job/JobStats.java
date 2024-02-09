package app.brecks.model.job;

import java.time.LocalDate;
import java.util.Collection;

public record JobStats (int totalManDays, double avgDailyManPower, Collection<LocalDate> missingReportDates) {}

// todo include totalActiveDays