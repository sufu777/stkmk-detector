package cc.sufuzz.stkmkdetector.detectors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Vector;

public class DetectResult {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<UnClosedStaticMockIssue> issues;

    public DetectResult() {
        startTime = LocalDateTime.now();
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<UnClosedStaticMockIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<UnClosedStaticMockIssue> issues) {
        this.issues = issues;
    }

    public Vector<Vector<Object>> getTableData() {
        return new Vector<>(issues.stream().map(UnClosedStaticMockIssue::toVector).toList());
    }
}
