package org.testvantage;

import java.util.Date;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
public class TestResult {

	@Id private String title;
	@Index private Date startTime;
	private long elapsedTime;
	private String responseText;
    private String goldStandard;

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

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public long getElapsedTime() {
		return elapsedTime;
	}

	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}

	public boolean isPassedTest() {
		return responseText != null && responseText.equals(goldStandard);
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

	public void save() {
		OfyService.ofy().save().entity(this).now();
	}

	public static TestResult load(String title) {
		return OfyService.ofy().load().type(TestResult.class).id(title).now();
	}
}
