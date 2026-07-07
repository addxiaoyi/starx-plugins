package io.github.addxiaoyi.starx.common.model;

import java.util.UUID;

public final class Report {

  private final String id;
  private final UUID reporterUuid;
  private final UUID targetUuid;
  private final String category;
  private final String details;
  private final String status;
  private final String resolvedBy;
  private final Long resolvedAt;

  public Report(String id, UUID reporterUuid, UUID targetUuid, String category,
      String details, String status, String resolvedBy, Long resolvedAt) {
    this.id = id;
    this.reporterUuid = reporterUuid;
    this.targetUuid = targetUuid;
    this.category = category;
    this.details = details;
    this.status = status;
    this.resolvedBy = resolvedBy;
    this.resolvedAt = resolvedAt;
  }

  public String id() { return id; }
  public UUID reporterUuid() { return reporterUuid; }
  public UUID targetUuid() { return targetUuid; }
  public String category() { return category; }
  public String details() { return details; }
  public String status() { return status; }
  public String resolvedBy() { return resolvedBy; }
  public Long resolvedAt() { return resolvedAt; }
}
