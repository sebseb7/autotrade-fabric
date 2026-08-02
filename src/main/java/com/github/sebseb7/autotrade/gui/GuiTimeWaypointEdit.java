package com.github.sebseb7.autotrade.gui;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * Time waypoint list editor based on malilib's GuiStringListEdit.
 * Adds Save/Cancel buttons and pre-fills new entries with current time/location.
 */
public class GuiTimeWaypointEdit extends GuiStringListEdit {
    private boolean saved = false;
    private final List<String> originalStrings;

    public GuiTimeWaypointEdit(IConfigStringList config, IDialogHandler dialogHandler, Screen parent) {
        super(config, null, dialogHandler, parent);
        this.title = StringUtils.translate("autotrade.gui.title.time_waypoints");
        this.originalStrings = new ArrayList<>(config.getStrings());
    }

    @Override
    public void initGui() {
        super.initGui();

        int buttonWidth = 60;
        int spacing = 10;
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = this.dialogLeft + this.dialogWidth / 2 - totalWidth / 2;
        int buttonY = this.dialogTop + this.dialogHeight - 26;

        ButtonGeneric saveButton = new ButtonGeneric(startX, buttonY, buttonWidth, 20,
                StringUtils.translate("autotrade.gui.button.save"));
        saveButton.setActionListener(new SaveButtonListener());
        this.addButton(saveButton, new SaveButtonListener());

        ButtonGeneric cancelButton = new ButtonGeneric(startX + buttonWidth + spacing, buttonY, buttonWidth, 20,
                StringUtils.translate("autotrade.gui.button.cancel"));
        cancelButton.setActionListener(new CancelButtonListener());
        this.addButton(cancelButton, new CancelButtonListener());
    }

    @Override
    public void removed() {
        if (!this.saved) {
            this.config.getStrings().clear();
            this.config.getStrings().addAll(this.originalStrings);
            this.config.markDirty();
            this.config.setModified();
        } else {
            if (this.getListWidget() != null && this.getListWidget().wereConfigsModified()) {
                this.getListWidget().applyPendingModifications();
            }
        }
    }

    @Override
    protected WidgetListStringListEdit createListWidget(int listX, int listY) {
        return new TimeWaypointListWidget(this.dialogLeft + 10, this.dialogTop + 20,
                this.getBrowserWidth(), this.getBrowserHeight(),
                this.dialogWidth - 100, this);
    }

    static String getDefaultWaypointValue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            //? if mc26 {
            long tick = mc.level.getDefaultClockTime() % 24000;
            //?} else {
            /*long tick = mc.level.getDayTime() % 24000;
            *///?}
            int x = (int) mc.player.getX();
            int y = (int) mc.player.getY();
            int z = (int) mc.player.getZ();
            return String.format("%d:%d,%d,%d", tick, x, y, z);
        }
        return "0:0,64,0";
    }

    private static class TimeWaypointListWidget extends WidgetListStringListEdit {

        TimeWaypointListWidget(int x, int y, int width, int height, int configWidth, GuiTimeWaypointEdit parent) {
            super(x, y, width, height, configWidth, parent);
        }

        @Override
        protected WidgetStringListEditEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String entry) {
            IConfigStringList config = this.getConfig();

            if (listIndex >= 0 && listIndex < config.getStrings().size()) {
                String actualValue = config.getStrings().get(listIndex);
                String defaultValue = config.getDefaultStrings().size() > listIndex
                        ? config.getDefaultStrings().get(listIndex) : "";
                
                // If the actual value is empty (just added via "+"), replace with default waypoint
                if (actualValue.isEmpty() || actualValue.isBlank()) {
                    String defaultVal = getDefaultWaypointValue();
                    config.getStrings().set(listIndex, defaultVal);
                    config.markDirty();
                    config.setModified();
                    actualValue = defaultVal;
                }
                
                return new TimeWaypointEntryWidget(x, y, this.browserEntryWidth, this.browserEntryHeight,
                        listIndex, isOdd, actualValue, defaultValue, this);
            } else {
                // Dummy entry (listIndex == -1) - used when list is empty
                String defaultValue = getDefaultWaypointValue();
                return new TimeWaypointEntryWidget(x, y, this.browserEntryWidth, this.browserEntryHeight,
                        listIndex, isOdd, defaultValue, defaultValue, this);
            }
        }
    }

    private static class TimeWaypointEntryWidget extends WidgetStringListEditEntry {
        TimeWaypointEntryWidget(int x, int y, int width, int height,
                int listIndex, boolean isOdd, String initialValue, String defaultValue,
                WidgetListStringListEdit parent) {
            super(x, y, width, height, listIndex, isOdd, initialValue, defaultValue, parent);
        }
    }

    private class SaveButtonListener implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            saved = true;
            closeEditor();
        }
    }

    private class CancelButtonListener implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            saved = false;
            closeEditor();
        }
    }

    private void closeEditor() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        if (this.dialogHandler != null) {
            // When opened as a dialog (within Liteloader config menu), use the dialog handler
            this.dialogHandler.closeDialog();
        } else {
            // When opened standalone, go back to the parent screen
            Screen parent = this.getParent();
            if (parent != null) {
                mc.setScreen(parent);
            } else {
                mc.setScreen(null);
            }
        }
    }
}
