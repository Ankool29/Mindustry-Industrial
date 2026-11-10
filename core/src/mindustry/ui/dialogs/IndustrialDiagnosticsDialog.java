package mindustry.ui.dialogs;

import arc.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.IndustrialDiagnostics.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.util.*;

/** Touch-first detached snapshots. Only explicit refresh reads the world; no per-frame economic scans. */
public class IndustrialDiagnosticsDialog extends BaseDialog{
    private final Planet planet;
    private Reading reading;
    private Sector selected;
    private Item material;
    private Platform platform;
    private String query = "";
    private Label freshness;
    private IndustrialGraphDialog graph;
    private long lastClock;

    public IndustrialDiagnosticsDialog(Planet planet){
        super("@industrial.diagnostics.title");
        if(!IndustrialDiagnostics.enabled(planet)) throw new IllegalArgumentException("Progressive industrial diagnostic only");
        this.planet = planet;
        shouldPause = true;
        addCloseListener();
        shown(this::refresh);
        hidden(() -> {reading = null;freshness = null;selected = null;material = null;platform = null;query = "";cont.clear();});
        onResize(this::rebuild);
        update(() -> {
            // Only a small age label is updated once per real second. No native rate/cache polling.
            if(freshness != null && Time.timeSinceMillis(lastClock) >= 1000){lastClock = Time.millis();freshness.setText(age());}
        });
    }

    private String n(float value){return Float.isFinite(value) ? Strings.fixed(value, 1) : Core.bundle.get("industrial.diagnostics.unknown");}
    private String f(String key, Object... values){return Core.bundle.format("industrial.diagnostics." + key, values);}
    private float width(){return Math.min(460f, Math.max(120f, Core.graphics.getWidth() / Scl.scl(1f) - 48f));}
    private void line(Table table, String value){table.add(value).left().wrap().growX().pad(5).row();}
    private String destination(Colony c){return c.destination == null ? Core.bundle.get("none") : c.destination.planet.localizedName + " / " + c.destination.name();}
    private String age(){
        String read = f("readage", Math.max(0L, (Time.nanos() - reading.readNanos) / 1_000_000_000L));
        Colony colony = selected == null ? null : reading.colony(selected);
        if(colony == null) return read;
        return read + "\n" + (colony.sampleNanos == 0L ? Core.bundle.get("industrial.diagnostics.sampleunknown") :
            f("sampleage", Math.max(0L, (Time.nanos() - colony.sampleNanos) / 1_000_000_000L)));
    }

    private void refresh(){
        reading = IndustrialDiagnostics.read(planet);
        if(selected != null && reading.colony(selected) == null){selected = null;material = null;platform = null;}
        if(platform != null){
            int position = platform.position;
            platform = reading.colony(selected).platforms.find(p -> p.position == position);
        }
        rebuild();
    }

    private void back(){
        if(material != null || platform != null){material = null;platform = null;}
        else if(selected != null){selected = null;}else{hide();return;}
        rebuild();
    }

    private void rebuild(){
        if(reading == null) return;
        cont.clear();buttons.clear();
        float buttonWidth = Math.min(180f, (width() - 8f) / 2f);
        buttons.button("@back", Icon.left, this::back).name("diagnostics-back").width(buttonWidth).height(64);
        buttons.button("@industrial.diagnostics.refresh", this::refresh).name("diagnostics-refresh").width(buttonWidth).height(64);
        Table body = new Table();body.defaults().growX();
        line(body, Core.bundle.get("industrial.diagnostics.readonly"));
        freshness = body.add(age()).left().wrap().growX().pad(5).get();body.row();lastClock = Time.millis();
        if(selected == null){
            TextField search = new TextField(query);search.setMessageText(Core.bundle.get("players.search"));
            Table list = new Table();
            search.changed(() -> {query = search.getText();colonies(list);});
            body.add(search).growX().pad(5).row();body.add(list).growX().row();colonies(list);
        }else{
            Colony c = reading.colony(selected);
            line(body, f("sector", c.sector.name()));
            line(body, Core.bundle.get("industrial.diagnostics." + (c.active ? c.paused ? "paused" : "active" : "simulated")));
            line(body, Core.bundle.get("industrial.diagnostics." + (c.active ? "observed" : "estimated")));
            if(material != null){material(body, c, c.material(material));}
            else if(platform != null){platform(body, c, platform);}
            else{colony(body, c);}
        }
        cont.pane(body).width(width()).growY().scrollX(false).name("diagnostics-scroll");
    }

    private void colonies(Table list){
        list.clear();list.defaults().growX();
        String filter = query.toLowerCase(Locale.ROOT);
        for(Colony c : reading.colonies){
            if(!c.sector.name().toLowerCase(Locale.ROOT).contains(filter)) continue;
            list.button(c.sector.name() + " — " + Core.bundle.get("industrial.diagnostics." + (c.active ? "shortactive" : "shortsimulated")), Styles.flatt, () -> {
                selected = c.sector;rebuild();
            }).name("diagnostics-sector-" + c.sector.id).with(b -> b.getLabel().setWrap(true)).minHeight(60).growX().pad(3).row();
        }
        if(reading.colonies.isEmpty()) line(list, Core.bundle.get("industrial.diagnostics.empty"));
    }

    private void colony(Table body, Colony c){
        body.button("Graphe économique — capture", () -> {
            if(graph == null)graph = new IndustrialGraphDialog();
            graph.show(c.sector);
        })
            .name("industrial-graph").minHeight(64).growX().with(b -> b.getLabel().setWrap(true)).row();
        line(body, f("destination", destination(c)));
        line(body, f("capacity", c.capacity));
        line(body, c.active ? "Stock réellement expédiable dans les rampes chargées : " + c.dispatchable : "Stock expédiable hors écran : aucun nouveau lot ; seuls les manifestes déjà engagés avancent.");
        line(body, c.transports);
        line(body, Core.bundle.get("industrial.diagnostics.consumptionlimit"));
        line(body, Core.bundle.get("industrial.diagnostics.ratelegend"));
        for(Material m : c.materials){
            body.button(b -> {
                b.image(m.item.uiIcon).size(28).padRight(8);
                b.add(m.item.localizedName + "\n" + f("stock", m.stock)).left().wrap().growX();
            }, Styles.flatt, () -> {material = m.item;rebuild();}).name("diagnostics-item-" + m.item.name).minHeight(64).growX().pad(3).row();
        }
        if(c.materials.isEmpty()) line(body, Core.bundle.get("industrial.diagnostics.nomaterials"));
        line(body, Core.bundle.get("industrial.diagnostics.platforms"));
        if(!c.platformsKnown){line(body, Core.bundle.get("industrial.diagnostics.platformunknown"));}
        else{
            if(c.platforms.isEmpty()) line(body, Core.bundle.get("industrial.diagnostics.noplatforms"));
            for(Platform p : c.platforms){
                body.button(p.block.localizedName + " — " + Point2.x(p.position) + ", " + Point2.y(p.position), Styles.flatt, () -> {platform = p;rebuild();})
                    .name("diagnostics-platform-" + p.position).with(b -> b.getLabel().setWrap(true)).minHeight(60).growX().pad(3).row();
            }
        }
    }

    private void material(Table body, Colony c, Material m){
        if(m == null){line(body, Core.bundle.get("industrial.diagnostics.nomaterials"));return;}
        line(body, m.item.localizedName);
        line(body, f("stock", m.stock));
        if(m.coreSaturated) line(body, Core.bundle.get("industrial.diagnostics.saturated"));
        line(body, Core.bundle.get("industrial.diagnostics." + (c.active ? "stockobserved" : "stockestimated")));
        line(body, f("gross", n(m.grossPerMinute)));
        line(body, f("net", n(m.netPerMinute)));
        line(body, f("withdrawal", n(m.netWithdrawalPerMinute)));
        line(body, Core.bundle.get("industrial.diagnostics.consumptionlimit"));
        line(body, f("exports", n(m.exportPerMinute)));
        line(body, f("received", n(m.receivedPerMinute)));
        line(body, f("offered", n(m.offeredPerMinute)));
        line(body, f("destination", destination(c)));
        line(body, Core.bundle.get("industrial.diagnostics.ratelegend"));
    }

    private void platform(Table body, Colony c, Platform p){
        line(body, p.block.localizedName);
        line(body, f("resource", p.resource == null ? Core.bundle.get("industrial.diagnostics.unassigned") : p.resource.localizedName));
        if(p.launch) line(body, f("destination", destination(c)));
        else line(body, "Réception de manifestes finis uniquement ; les offres moyennes ne sont pas utilisées.");
        line(body, f("nominal", n(p.nominalPerMinute)));
        line(body, Core.bundle.get("industrial.diagnostics.padunmeasured"));
        Material m = p.resource == null ? null : c.material(p.resource);
        if(m != null) line(body, p.launch ? f("exports", n(m.exportPerMinute)) : f("offered", n(m.offeredPerMinute)));
        line(body, f("buffer", p.buffered, p.capacity));
        for(ItemStack stack : p.cargo) line(body, stack.item.localizedName + " : " + stack.amount);
        line(body, f("efficiency", n(p.efficiency * 100f)));
        if(p.launch) line(body, f("charge", n(Math.min(p.charge, 1f) * 100f)));
        if(p.liquidType != null){
            line(body, f("liquid", p.liquidType.localizedName, n(p.liquid)));
            line(body, p.launch ? f("liquidrate", n(p.liquidPerSecond)) : f("liquidrequired", n(p.requiredLiquid)));
        }
        if(p.arriving != null) line(body, f("localarrival", p.arriving.localizedName));
        for(String condition : p.conditions) line(body, Core.bundle.get("industrial.diagnostics.condition." + condition));
        line(body, Core.bundle.get("industrial.diagnostics.transitlimit"));
    }
}
