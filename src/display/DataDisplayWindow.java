package display;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataDisplayWindow extends JFrame {

  boolean highLiquiditySwiped;

  boolean lowLiquiditySwiped;

  long highLiquiditySwipedTime;

  long lowLiquiditySwipedTime;

  SimpleDateFormat format = new SimpleDateFormat("dd/MMM/yyyy hh:mm:ss a");

  JLabel label = new JLabel();

  public DataDisplayWindow() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    showChartFrame();
  }

  public void setHighLiquiditySwiped(boolean highLiquiditySwiped) {
    this.highLiquiditySwiped = highLiquiditySwiped;
  }

  public void setLowLiquiditySwiped(boolean highLiquiditySwiped) {
    this.lowLiquiditySwiped = highLiquiditySwiped;
  }

  public void setHighLiquiditySwipedTime(long highLiquiditySwipedTime) {
    this.highLiquiditySwipedTime = highLiquiditySwipedTime;
  }

  public void setLowLiquiditySwipedTime(long lowLiquiditySwipedTime) {
    this.lowLiquiditySwipedTime = lowLiquiditySwipedTime;
  }

  public void showChartFrame() {
    setSize(100, 100);
    centerFrame();
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    label.setText(getOutputString());
    panel.add(label);
    getContentPane().add(panel);
    setVisible(true);
  }

  private String getOutputString() {
    String highLiquidityTimeString = highLiquiditySwipedTime == 0 ? "" : format.format(new Date(highLiquiditySwipedTime));
    String lowLiquidityTimeString = lowLiquiditySwipedTime == 0 ? "" : format.format(new Date(lowLiquiditySwipedTime));

    return "<html>High Liquidity Swiped: " + highLiquiditySwiped + "<br/>" +
            "High Liquidity Swiped Time: " + highLiquidityTimeString + "<br/>" +
            "Low Liquidity Swiped: " + lowLiquiditySwiped + "<br/>" +
            "Low Liquidity Swiped Time: " + lowLiquidityTimeString +
            "</html>";
  }

  public void update() {
    label.setText(getOutputString());
    //revalidate();
    repaint();
  }

  private void centerFrame() {
    Toolkit tk = Toolkit.getDefaultToolkit();
    Dimension screenSize = tk.getScreenSize();
    int screenHeight = screenSize.height;
    int screenWidth = screenSize.width;
    setSize(screenWidth / 4, screenHeight / 4);
    setLocation(screenWidth/2, screenHeight/2);
  }
}
