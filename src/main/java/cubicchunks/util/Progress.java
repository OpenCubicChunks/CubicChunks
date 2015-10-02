/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.util;

import java.util.LinkedList;

import cubicchunks.TallWorldsMod;

public class Progress {
	
	private static final int LogTimeoutMilliseconds = 5 * 60 * 1000; // 5 minutes
	private static final int DefaultReportIntervalMilliseconds = 5 * 1000; // 5 seconds
	private static final int UpdateIntervalMilliseconds = 500; // half a second
	
	private static class LogEntry {
		
		public long work;
		public long time;
		
		public LogEntry(long work, long time) {
			this.work = work;
			this.time = time;
		}
	}

	private long m_totalWork;
	private long m_reportIntervalMilliseconds;
	private long m_currentWork;
	private Timer m_timer;
	private LinkedList<LogEntry> m_workLog;
	private long m_lastReportMilliseconds;
	private boolean m_isOkToReport;
	
	
	public Progress(long totalWork) {
		this(totalWork, DefaultReportIntervalMilliseconds);
	}
	
	public Progress(long totalWork, long reportIntervalMilliseconds) {
		
		// save params
		m_totalWork = totalWork;
		m_reportIntervalMilliseconds = reportIntervalMilliseconds;
		
		// init defaults
		m_currentWork = 0;
		m_timer = new Timer();
		m_workLog = new LinkedList<LogEntry>();
		m_lastReportMilliseconds = 0;
		m_timer.start();
		m_isOkToReport = false;
		
		// add the 0,0 point to the work log
		m_workLog.addLast(new LogEntry(m_currentWork, m_timer.getElapsedMilliseconds()));
	}
	
	public long getNumWorkDone() {
		return m_currentWork;
	}
	
	public long getTotalWork() {
		return m_totalWork;
	}
	
	public boolean isFinished() {
		return m_currentWork == m_totalWork;
	}
	
	public boolean isOkToReport() {
		return m_isOkToReport;
	}
	
	public boolean setProgress(long currentWork) {
		m_currentWork = currentWork;
		
		// should we update and/or report?
		long elapsedMilliseconds = m_timer.getElapsedMilliseconds();
		boolean update = elapsedMilliseconds - m_workLog.getLast().time >= UpdateIntervalMilliseconds;
		boolean report = elapsedMilliseconds - m_lastReportMilliseconds >= m_reportIntervalMilliseconds;
		
		// if this is the last work done, force an update and a report
		if (isFinished()) {
			m_timer.stop();
			update = true;
			report = true;
		}
		
		// update the work log if needed
		if (update) {
			m_workLog.addLast(new LogEntry(m_currentWork, elapsedMilliseconds));
		}
		
		// report the progress if needed
		if (report) {
			// recalculate statistics
			double complete = (double)m_currentWork / (double)m_totalWork;
			
			// build the message
			StringBuilder msg = new StringBuilder();
			msg.append("Progress: ");
			msg.append(formatPercent(complete));
			msg.append("\tETA: ");
			msg.append(formatTimeInterval(getEta()));
			
			TallWorldsMod.LOGGER.info(msg.toString());
			
			pruneLog();
			m_lastReportMilliseconds = elapsedMilliseconds;
		}
		m_isOkToReport = report;
		
		// add the finished message if needed
		if (m_currentWork == m_totalWork) {
			TallWorldsMod.LOGGER.info("Finished in " + m_timer.getElapsedTime());
		}
		
		return report;
	}
	
	public boolean incrementProgress() {
		return incrementProgress(1);
	}
	
	public boolean incrementProgress(int numWorkDone) {
		return setProgress(m_currentWork + numWorkDone);
	}
	
	public float getElapsedSeconds() {
		return m_timer.getElapsedSeconds();
	}
	
	public String getElapsedTime() {
		return m_timer.getElapsedTime();
	}
	
	private void pruneLog() {
		
		// remove old entries from the log if needed
		long elapsedMilliseconds = m_workLog.getLast().time;
		while (m_workLog.size() > 1 && elapsedMilliseconds - m_workLog.getFirst().time > LogTimeoutMilliseconds) {
			m_workLog.removeFirst();
		}
	}
	
	private long getEta() {
		
		// not enough data?
		if (m_workLog.size() < 2) {
			return -1;
		}
		
		// perform simple linear regression
		double sumxy = 0.0;
		double sumx = 0.0;
		double sumy = 0.0;
		double sumxsq = 0.0;
		for (LogEntry entry : m_workLog) {
			sumxy += entry.work * entry.time;
			sumx += entry.work;
			sumy += entry.time;
			sumxsq += entry.work * entry.work;
		}

		// solve for slope (a) and intercept (b)
		double a = (sumxy - sumx*sumy/m_workLog.size()) / (sumxsq - sumx*sumx/m_workLog.size());
		double b = (sumy - a*sumx)/m_workLog.size();
		
		// extrapolate the finish time (y = ax+b), then compute the ETA
		double x = m_totalWork;
		return (long)(a*x + b) - m_workLog.getLast().time;
	}
	
	private String formatPercent(double percent) {
		return String.format("%5.1f", percent * 100.0) + "%";
	}
	
	private String formatTimeInterval(long milliseconds) {
		
		if (milliseconds < 0) {
			return "calculating...";
		}
		
		long seconds = milliseconds / 1000;
		long hours = seconds / 3600;
		long minutes = (seconds - hours * 3600) / 60;
		seconds = seconds - hours * 3600 - minutes * 60;
		
		StringBuffer buf = new StringBuffer();
		buf.append(hours < 10 ? "0" + hours : hours);
		buf.append(":");
		buf.append(minutes < 10 ? "0" + minutes : minutes);
		buf.append(":");
		buf.append(seconds < 10 ? "0" + seconds : seconds);
		
		return buf.toString();
	}
}
