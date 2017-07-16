package main;

import com.dukascopy.api.*;
import com.dukascopy.api.feed.IFeedDescriptor;
import com.dukascopy.api.system.ISystemListener;
import com.dukascopy.api.system.ITesterClient;
import com.dukascopy.api.system.TesterFactory;
import com.dukascopy.api.system.tester.ITesterExecution;
import com.dukascopy.api.system.tester.ITesterExecutionControl;
import com.dukascopy.api.system.tester.ITesterGui;
import com.dukascopy.api.system.tester.ITesterUserInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import strategy.LondonOpenKillZoneStrategy;
import utils.SetupHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

@SuppressWarnings({"serial", "Duplicates"})
public class TesterGUIMode extends JFrame implements ITesterUserInterface, ITesterExecution {
  private static final Logger LOGGER = LoggerFactory.getLogger(TesterGUIMode.class);

  private final int frameWidth = 1000;
  private final int frameHeight = 600;
  private final int controlPanelHeight = 40;

  private JPanel currentChartPanel = null;
  private ITesterExecutionControl executionControl = null;

  private JPanel controlPanel = null;
  private JButton startStrategyButton = null;
  private JButton pauseButton = null;
  private JButton continueButton = null;
  private JButton cancelButton = null;

  private Instrument instrument = Instrument.EURUSD;

  public TesterGUIMode() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
  }

  public void startStrategy() throws Exception {
    final ITesterClient client = TesterFactory.getDefaultInstance();

    client.setSystemListener(new ISystemListener() {
      @Override
      public void onStart(long processId) {
        LOGGER.info("Strategy started: " + processId);
        updateButtons();
      }

      @Override
      public void onStop(long processId) {
        LOGGER.info("Strategy stopped: " + processId);
        resetButtons();

        File reportFile = new File("D:\\report.html");
        try {
          client.createReport(processId, reportFile);
        } catch (Exception e) {
          LOGGER.error(e.getMessage(), e);
        }
        if (client.getStartedStrategies().size() == 0) {
          //Do nothing
        }
      }

      @Override
      public void onConnect() {
        LOGGER.info("Connected");
      }

      @Override
      public void onDisconnect() {
        //tester doesn't disconnect
      }
    });

    SetupHelper.connectToServer(client, LOGGER);


    final Set<Instrument> instruments = new HashSet<>();
    instruments.add(instrument);
    instruments.add(Instrument.AUDCAD);

    LOGGER.info("Subscribing instruments...");
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.DAY_OF_MONTH, 1);
    Calendar to = (Calendar) cal.clone();
    to.set(Calendar.DAY_OF_MONTH, 28);
    Calendar today = Calendar.getInstance();
    client.setDataInterval(ITesterClient.DataLoadingMethod.ALL_TICKS, cal.getTimeInMillis(), today.getTimeInMillis());
    //client.setDataInterval(Period.ONE_HOUR, OfferSide.BID, ITesterClient.InterpolationMethod.OPEN_TICK, cal.getTimeInMillis(), today.getTimeInMillis());
    client.setSubscribedInstruments(instruments);
    //setting initial deposit
    client.setInitialDeposit(Instrument.EURUSD.getSecondaryJFCurrency(), 50000);

    //load data
    LOGGER.info("Downloading data");
    Future<?> future = client.downloadData(null);
    //wait for downloading to complete
    future.get();
    //start the strategy
    LOGGER.info("Starting strategy");

    client.startStrategy(
        new LondonOpenKillZoneStrategy(),
        new LoadingProgressListener() {
          @Override
          public void dataLoaded(long startTime, long endTime, long currentTime, String information) {
            LOGGER.info(information);
          }

          @Override
          public void loadingFinished(boolean allDataLoaded, long startTime, long endTime, long currentTime) {
          }

          @Override
          public boolean stopJob() {
            return false;
          }
        }, this, this
    );
    //now it's running
  }

  @Override
  public void setChartPanels(Map<IChart, ITesterGui> chartPanels) {
    for (Map.Entry<IChart, ITesterGui> entry : chartPanels.entrySet()) {
      IChart chart = entry.getKey();
      IFeedDescriptor fd = chart.getFeedDescriptor();
      fd.setPeriod(Period.ONE_HOUR);
      chart.setFeedDescriptor(fd);
      JPanel chartPanel = entry.getValue().getChartPanel();
      if (chart.getFeedDescriptor().getInstrument().equals(instrument)) {
        setTitle(chart.getFeedDescriptor().toString());
        addChartPanel(chartPanel);
        break;
      }
    }
  }

  @Override
  public void setExecutionControl(ITesterExecutionControl executionControl) {
    this.executionControl = executionControl;
  }

  /**
   * Center a frame on the screen
   */
  private void centerFrame() {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screenSize = tk.getScreenSize();
    int screenHeight = screenSize.height;
    int screenWidth = screenSize.width;
    setSize(screenWidth / 2, screenHeight / 2);
    setLocation(screenWidth / 4, screenHeight / 4);
  }

  private void addChartPanel(JPanel chartPanel) {
    removecurrentChartPanel();

    this.currentChartPanel = chartPanel;
    chartPanel.setPreferredSize(new Dimension(frameWidth, frameHeight - controlPanelHeight));
    chartPanel.setMinimumSize(new Dimension(frameWidth, 200));
    chartPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    getContentPane().add(chartPanel);
    this.validate();
    chartPanel.repaint();
  }

  private void removecurrentChartPanel() {
    if (this.currentChartPanel != null) {
      try {
        SwingUtilities.invokeAndWait(new Runnable() {
          @Override
          public void run() {
            TesterGUIMode.this.getContentPane().remove(TesterGUIMode.this.currentChartPanel);
            TesterGUIMode.this.getContentPane().repaint();
          }
        });
      } catch (Exception e) {
        LOGGER.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Add buttons to start/pause/continue/cancel actions
   */
  private void addControlPanel() {
    controlPanel = new JPanel();
    FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
    controlPanel.setLayout(flowLayout);
    controlPanel.setPreferredSize(new Dimension(frameWidth, controlPanelHeight));
    controlPanel.setMinimumSize(new Dimension(frameWidth, controlPanelHeight));
    controlPanel.setMaximumSize(new Dimension(Short.MAX_VALUE, controlPanelHeight));

    startStrategyButton = new JButton("Start strategy");
    startStrategyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        startStrategyButton.setEnabled(false);
        Runnable r = new Runnable() {
          public void run() {
            try {
              startStrategy();
            } catch (Exception e2) {
              LOGGER.error(e2.getMessage(), e2);
              e2.printStackTrace();
              resetButtons();
            }
          }
        };
        Thread t = new Thread(r);
        t.start();
      }
    });

    pauseButton = new JButton("Pause");
    pauseButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (executionControl != null) {
          executionControl.pauseExecution();
          updateButtons();
        }
      }
    });

    continueButton = new JButton("Continue");
    continueButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (executionControl != null) {
          executionControl.continueExecution();
          updateButtons();
        }
      }
    });

    cancelButton = new JButton("Cancel");
    cancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (executionControl != null) {
          executionControl.cancelExecution();
          updateButtons();
        }
      }
    });

    controlPanel.add(startStrategyButton);
    controlPanel.add(pauseButton);
    controlPanel.add(continueButton);
    controlPanel.add(cancelButton);
    getContentPane().add(controlPanel);

    pauseButton.setEnabled(false);
    continueButton.setEnabled(false);
    cancelButton.setEnabled(false);
  }

  private void updateButtons() {
    if (executionControl != null) {
      startStrategyButton.setEnabled(executionControl.isExecutionCanceled());
      pauseButton.setEnabled(!executionControl.isExecutionPaused() && !executionControl.isExecutionCanceled());
      cancelButton.setEnabled(!executionControl.isExecutionCanceled());
      continueButton.setEnabled(executionControl.isExecutionPaused());
    }
  }

  private void resetButtons() {
    startStrategyButton.setEnabled(true);
    pauseButton.setEnabled(false);
    continueButton.setEnabled(false);
    cancelButton.setEnabled(false);
  }

  public void showChartFrame() {
    setSize(frameWidth, frameHeight);
    centerFrame();
    addControlPanel();
    setVisible(true);
  }

  public static void main(String[] args) throws Exception {
    TesterGUIMode testerMainGUI = new TesterGUIMode();
    testerMainGUI.showChartFrame();
  }
}