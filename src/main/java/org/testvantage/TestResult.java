package org.testvantage;

import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class TestResult {

	public enum Status {
		UNKNOWN,
		PASSED,
		FAILED,
		ERROR
	}

	@Id private String title;
	@Index private String suiteId;
	@Index private String scenarioId;
	@Index private String targetUrl;
	@Index private Date startTime;
	@Index private Date completedAt;
	@Index private String status;
	private long elapsedTime;
	private String responseText;
    private String goldStandard;
	private String summary;
	private String details;

	public TestResult() {
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getStartTime() {
		return startTime;
	}

	public String getSuiteId() {
		return suiteId;
	}

	public void setSuiteId(String suiteId) {
		this.suiteId = suiteId;
	}

	public String getScenarioId() {
		return scenarioId;
	}

	public void setScenarioId(String scenarioId) {
		this.scenarioId = scenarioId;
	}

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	public Date getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(Date completedAt) {
		this.completedAt = completedAt;
	}

	public String getStatus() {
		return status == null ? Status.UNKNOWN.name() : status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isPassedTest() {
		if (status != null) {
			return Status.PASSED.name().equals(status);
		}
		return responseText != null && responseText.equals(goldStandard);
	}

	public boolean isFailedTest() {
		return Status.FAILED.name().equals(getStatus()) || Status.ERROR.name().equals(getStatus());
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public String getGoldStandard() {
		return goldStandard;
	}

	public void setGoldStandard(String goldStandard) {
		this.goldStandard = goldStandard;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDetails() {
		return details;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	public void markPassed(String summary) {
		status = Status.PASSED.name();
		this.summary = summary;
		completedAt = new Date();
	}

	public void markFailed(String summary, String details) {
		status = Status.FAILED.name();
		this.summary = summary;
		this.details = details;
		completedAt = new Date();
	}

	public void markError(String summary, String details) {
		status = Status.ERROR.name();
		this.summary = summary;
		this.details = details;
		completedAt = new Date();
	}

	public void save() {
		OfyService.ofy().save().entity(this).now();
	}

	public static TestResult load(String title) {
		return OfyService.ofy().load().type(TestResult.class).id(title).now();
	}
}
