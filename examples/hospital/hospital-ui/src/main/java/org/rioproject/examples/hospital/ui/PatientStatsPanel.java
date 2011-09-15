/*
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rioproject.examples.hospital.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RectangleInsets;
import org.rioproject.examples.hospital.CalculablePatient;
import org.rioproject.examples.hospital.Patient;
import org.rioproject.resources.servicecore.Service;
import org.rioproject.watch.Calculable;
import org.rioproject.watch.WatchDataSource;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Displays patient stats.
 */
public class PatientStatsPanel extends JPanel {
    private TimeSeries pulseTimeSeries = new TimeSeries("Pulse", FixedMillisecond.class);
	private TimeSeries temperatureTimeSeries = new TimeSeries("Temperature", FixedMillisecond.class);
    private ScheduledExecutorService scheduler;
    private Service service;
    private CalculablePatient lastPulse;
    private Calculable lastTemperature;
    private static final long MINUTE=60*1000;
    private JLabel patientLabel;

    public PatientStatsPanel() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(2, 8, 8, 8));
        patientLabel = new JLabel(getLabelText(null));
        add(patientLabel, BorderLayout.NORTH);
        pulseTimeSeries.setMaximumItemAge(5*MINUTE);
        temperatureTimeSeries.setMaximumItemAge(5*MINUTE);
        TimeSeriesCollection pulseDataSet = new TimeSeriesCollection(pulseTimeSeries);
		TimeSeriesCollection temperatureDataSet = new TimeSeriesCollection(temperatureTimeSeries);
        JFreeChart pulseChart = createTimeSeriesChart(pulseDataSet, Color.RED);
        JFreeChart temperatureChart = createTimeSeriesChart(temperatureDataSet, Color.BLUE);

        JPanel chartPanel = new JPanel(new GridLayout(1, 2));
        chartPanel.add(makeChartPanel(pulseChart));
        chartPanel.add(makeChartPanel(temperatureChart));
        add(chartPanel, BorderLayout.CENTER);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new FeederTask(),
                                      0,
                                      2,
                                      TimeUnit.SECONDS);
    }

    private String getLabelText(Patient p) {
        if(p==null)
            return "<html><body><h3>Patient Stats</h3></body></html>";
        else
            return "<html><body><h3>Patient Stats for "+p.getPatientInfo().getName()+"</h3></body></html>";
    }
    
    void setPatient(Patient p) {
        if(p==null) {
            pulseTimeSeries.clear();
            temperatureTimeSeries.clear();
            patientLabel.setText(getLabelText(p));
            return;
        }
        patientLabel.setText(getLabelText(p));
        service = (Service)p.getBed();
        try {
            pulseTimeSeries.clear();
            WatchDataSource pulse = service.fetch("pulse");
            for(Calculable c : pulse.getCalculable()) {
                lastPulse = (CalculablePatient)c;
                pulseTimeSeries.addOrUpdate(new FixedMillisecond(c.getWhen()), c.getValue());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        try {
            temperatureTimeSeries.clear();
            WatchDataSource temperature = service.fetch("temperature");
            for(Calculable c : temperature.getCalculable()) {
                lastTemperature = c;
                temperatureTimeSeries.add(new FixedMillisecond(c.getWhen()), c.getValue());
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void shutdown() {
        if(scheduler!=null)
            scheduler.shutdownNow();
        scheduler = null;
    }

    private Service getService() {
        return service;
    }

    class FeederTask implements Runnable {
        
        public void run() {
            Service s = getService();
            if(s!=null) {
                try {
                    CalculablePatient c = checkAdd(s, "pulse", lastPulse);
                    if(c!=null) {
                        lastPulse = c;
                        pulseTimeSeries.addOrUpdate(new FixedMillisecond(c.getWhen()), c.getValue());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    Calculable c = checkAdd(s, "temperature", lastTemperature);
                    if(c!=null) {
                        lastTemperature = c;
                        temperatureTimeSeries.addOrUpdate(new FixedMillisecond(c.getWhen()), c.getValue());
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }

        private CalculablePatient checkAdd(Service s, String watch, Calculable lastOne) throws
                                                                              RemoteException {
            WatchDataSource wds = s.fetch(watch);
            CalculablePatient c = (CalculablePatient)wds.getLastCalculable();
            Date lastMeasurement = new Date(lastOne.getWhen());
            Date currentMeasurement = new Date(c.getWhen());
            return currentMeasurement.after(lastMeasurement)?c:null;
        }
    }

    private ChartPanel makeChartPanel(JFreeChart chart) {
        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new Dimension(300, 200));
        return chartPanel;
    }
    
    private JFreeChart createTimeSeriesChart(TimeSeriesCollection dataSet,
                                             Color color) {
		JFreeChart chart = ChartFactory.createTimeSeriesChart(
	            "",
	            "",
	            "",
	            dataSet,
	            true,
	            true,
	            false
	        );


		XYPlot plot = (XYPlot) chart.getPlot();
        plot.getRenderer().setSeriesPaint(0, color);
		plot.setBackgroundPaint(Color.lightGray);
		plot.setDomainGridlinePaint(Color.white);
		plot.setRangeGridlinePaint(Color.white);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainCrosshairVisible(true);
		plot.setRangeCrosshairVisible(true);

        ValueAxis yAxis = plot.getRangeAxis();
        yAxis.setRange(0, 150);
        ValueAxis xAxis = plot.getDomainAxis();
        xAxis.setAutoRange(true);
        xAxis.setFixedAutoRange(5*MINUTE);
		return chart;
	}
}
