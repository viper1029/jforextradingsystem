package utils;


import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ISystemListener;
import constants.Constants;
import org.slf4j.Logger;

public class SetupHelper {

  public static IClient getClient(final Logger logger) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
    final IClient client = ClientFactory.getDefaultInstance();
    client.setSystemListener(new ISystemListener() {
      private int lightReconnects = 3;

      @Override
      public void onStart(long processId) {
        logger.info("Strategy started: " + processId);
      }

      @Override
      public void onStop(long processId) {
        logger.info("Strategy stopped: " + processId);
        if (client.getStartedStrategies().size() == 0) {
          System.exit(0);
        }
      }

      @Override
      public void onConnect() {
        logger.info("Connected");
        lightReconnects = 3;
      }

      @Override
      public void onDisconnect() {
        logger.warn("Disconnected");
        if (lightReconnects > 0) {
          client.reconnect();
          --lightReconnects;
        }
        else {
          try {
            Thread.sleep(5000);
          }
          catch (InterruptedException e) {
            //ignore
          }
          try {
            client.connect(Constants.JNLP_URL, Constants.USER_NAME, Constants.PASSWORD);
          }
          catch (Exception e) {
            logger.error(e.getMessage(), e);
          }
        }
      }
    });

    return client;
  }

  public static void connectToServer(IClient client, Logger logger) throws Exception {
    client.connect(Constants.JNLP_URL, Constants.USER_NAME, Constants.PASSWORD);

    int i = 10; //wait max ten seconds
    while (i > 0 && !client.isConnected()) {
      Thread.sleep(1000);
      i--;
    }
    if (!client.isConnected()) {
      logger.error("Failed to connect Dukascopy servers");
      System.exit(1);
    }
  }
}
