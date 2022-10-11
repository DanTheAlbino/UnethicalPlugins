package net.runelite.client.plugins.avorkath;

import com.openosrs.client.ui.overlay.components.table.TableAlignment;
import com.openosrs.client.ui.overlay.components.table.TableComponent;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
class AVorkathOverlay extends OverlayPanel {
   private static final Logger log = LoggerFactory.getLogger(AVorkathOverlay.class);
   private final AVorkathPlugin plugin;
   private final AVorkathConfig config;
   String timeFormat;
   private String infoStatus = "Starting...";

   @Inject
   private AVorkathOverlay(Client client, AVorkathPlugin plugin, AVorkathConfig config) {
      super(plugin);
      this.setPosition(OverlayPosition.BOTTOM_LEFT);
      this.plugin = plugin;
      this.config = config;
      this.getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "airs overlay"));
   }

   public Dimension render(Graphics2D graphics) {
      if (this.plugin.botTimer != null && this.plugin.startTeaks) {
         TableComponent tableComponent = new TableComponent();
         tableComponent.setColumnAlignments(new TableAlignment[]{TableAlignment.LEFT, TableAlignment.RIGHT});
         Duration duration = Duration.between(this.plugin.botTimer, Instant.now());
         this.timeFormat = duration.toHours() < 1L ? "mm:ss" : "HH:mm:ss";
         tableComponent.addRow(new String[]{"Time running:", DurationFormatUtils.formatDuration(duration.toMillis(), this.timeFormat)});
         if (this.plugin.state != null && !this.plugin.state.name().equals("TIMEOUT")) {
            this.infoStatus = this.plugin.state.name();
         }

         tableComponent.addRow(new String[]{"Status:", this.infoStatus});
         TableComponent tableDelayComponent = new TableComponent();
         tableDelayComponent.setColumnAlignments(new TableAlignment[]{TableAlignment.LEFT, TableAlignment.RIGHT});
         if (!tableComponent.isEmpty()) {
            this.panelComponent.setBackgroundColor(ColorUtil.fromHex("#121212"));
            this.panelComponent.setPreferredSize(new Dimension(200, 200));
            this.panelComponent.setBorder(new Rectangle(5, 5, 5, 5));
            this.panelComponent.getChildren().add(TitleComponent.builder().text("Anarchise Vorkath").color(ColorUtil.fromHex("#40C4FF")).build());
            this.panelComponent.getChildren().add(tableComponent);
            this.panelComponent.getChildren().add(tableDelayComponent);
         }

         return super.render(graphics);
      } else {
         log.debug("Overlay conditions not met, not starting overlay");
         return null;
      }
   }
}
