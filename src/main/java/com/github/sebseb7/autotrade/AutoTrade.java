package com.github.sebseb7.autotrade;

import fi.dy.masa.malilib.event.InitializationHandler;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AutoTrade implements ModInitializer {
  public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

  @Override
  public void onInitialize() {
    InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
  }
}
