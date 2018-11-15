package org.openstreetmap.josm.plugins.pt_assistant.utils;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.Notification;

public final class NotificationUtils {
    private NotificationUtils() {
        // Private constructor to avoid instatiation
    }

    /**
     * Shows a notification that downloading starts, then executes {@link Future#get()} on the given {@link Future},
     * then shows another notification that downloading has finished.
     * @param downloadFuture the {@link Future} doing the downloading
     * @param translatedLabel a label for the download that will be shown as part of both notifications
     *     to differentiate from other downloads
     * @throws ExecutionException whenever {@link Future#get()} throws an exception, it is rethrown
     * @throws InterruptedException whenever {@link Future#get()} throws an exception, it is rethrown
     */
    public static void downloadWithNotifications(final Future<?> downloadFuture, final String translatedLabel) throws ExecutionException, InterruptedException {
        new Notification(tr("Download started: {0}", translatedLabel))
            .setIcon(JOptionPane.INFORMATION_MESSAGE)
            .setDuration(Notification.TIME_SHORT)
            .show();
        downloadFuture.get();
        new Notification(tr("Download finished: {0}", translatedLabel))
            .setIcon(JOptionPane.INFORMATION_MESSAGE)
            .setDuration(Notification.TIME_SHORT)
            .show();
    }
}
