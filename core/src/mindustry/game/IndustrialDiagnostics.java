package mindustry.game;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/** Detached, read-only views of native estimates. No economic tick, cache refresh or settings write. */
public final class IndustrialDiagnostics{
    private static final ObjectMap<Sector, Stamp> stamps = new ObjectMap<>();
    private static Settings session;
    private IndustrialDiagnostics(){}

    private static class Stamp{
        SectorInfo info;
        long nanos;
    }

    public static boolean enabled(Planet planet){return planet != null && planet == Planets.industrialSerpuloProgressive;}

    private static void session(){
        if(session != Core.settings){stamps.clear(); session = Core.settings;}
    }

    /** Called only after a native statistics sample, once per simulation second. Session memory only. */
    public static void measured(Sector sector){
        if(sector == null || !enabled(sector.planet) || sector.id < 0 || sector.id >= sector.planet.sectors.size || sector.planet.sectors.get(sector.id) != sector) return;
        session();
        Stamp stamp = stamps.get(sector);
        if(stamp == null){stamp = new Stamp(); stamps.put(sector, stamp);}
        stamp.info = sector.info;
        stamp.nanos = Time.nanos();
    }

    public static long lastSample(Sector sector){
        if(!enabled(sector.planet)) return 0L;
        session();
        Stamp stamp = stamps.get(sector);
        return stamp != null && stamp.info == sector.info ? stamp.nanos : 0L;
    }

    /** A native mean observed while active becomes a projection when the world is not active. */
    public enum Evidence{activeObservation, nativeEstimate}

    public static class Material{
        public Item item;
        public int stock;
        public boolean coreSaturated;
        public float grossPerMinute, netPerMinute, netWithdrawalPerMinute;
        public float exportPerMinute, receivedPerMinute, offeredPerMinute;
    }

    public static class Platform{
        public Block block;
        public int position, buffered, capacity;
        public Item resource, arriving;
        public float nominalPerMinute, efficiency, charge, cooldown;
        public float liquid, requiredLiquid, liquidPerSecond;
        public Liquid liquidType;
        public boolean launch, enabled;
        public final Seq<ItemStack> cargo = new Seq<>();
        public final Seq<String> conditions = new Seq<>();
        // There is deliberately no per-platform observed rate or inferred in-flight manifest.
    }

    public static class Colony{
        public Sector sector, destination;
        public Evidence evidence;
        public boolean active, paused, legacy, platformsKnown;
        public int capacity;
        public long sampleNanos;
        public final Seq<Material> materials = new Seq<>();
        public final Seq<Platform> platforms = new Seq<>();
        public Material material(Item item){return materials.find(m -> m.item == item);}
    }

    public static class Reading{
        public Planet planet;
        public long readNanos;
        public final Seq<Colony> colonies = new Seq<>();
        public Colony colony(Sector sector){return colonies.find(c -> c.sector == sector);}
    }

    private static float mean(ObjectMap<Item, SectorInfo.ExportStat> values, Item item){
        var stat = values.get(item);
        return stat == null ? 0f : stat.mean * 60f;
    }

    private static boolean hasExports(SectorInfo info){
        float sum = 0f;
        for(var stat : info.export.values()) sum += stat.mean;
        return sum >= 0.01f;
    }

    public static Reading read(Planet planet){
        if(!enabled(planet)) throw new IllegalArgumentException("Progressive industrial diagnostic only");
        Reading result = new Reading();result.planet = planet;result.readNanos = Time.nanos();
        ObjectMap<Sector, float[]> offers = new ObjectMap<>();
        // Same eligibility as eachImport, without getImportRates/refreshImportRates (which mutate caches).
        for(Sector source : planet.sectors){
            Sector target = source.info.destination;
            if(!source.hasBase() || target == null || target.planet != planet || source.info == target.info || !hasExports(source.info)) continue;
            float[] rates = offers.get(target, () -> new float[content.items().size]);
            source.info.export.each((item, stat) -> rates[item.id] += stat.mean * 60f);
        }
        for(Sector sector : planet.sectors){
            if(!sector.hasBase()) continue;
            Colony colony = new Colony();colony.sector = sector;colony.destination = sector.info.destination;
            colony.active = sector.isBeingPlayed();colony.paused = colony.active && state.isPaused();
            colony.evidence = colony.active ? Evidence.activeObservation : Evidence.nativeEstimate;
            colony.sampleNanos = lastSample(sector);colony.legacy = planet.campaignRules.legacyLaunchPads;
            colony.capacity = colony.active && state.rules.defaultTeam.core() != null ? state.rules.defaultTeam.core().storageCapacity : sector.info.storageCapacity;
            ItemSeq stock = sector.items();float[] incoming = offers.get(sector);
            for(Item item : content.items()){
                Material row = new Material();row.item = item;row.stock = stock.get(item);row.coreSaturated = colony.capacity > 0 && row.stock >= colony.capacity;
                row.grossPerMinute = mean(sector.info.rawProduction, item);
                row.netPerMinute = mean(sector.info.production, item);
                row.netWithdrawalPerMinute = Math.max(0f, -row.netPerMinute);
                row.exportPerMinute = mean(sector.info.export, item);
                row.receivedPerMinute = mean(sector.info.imports, item);
                row.offeredPerMinute = incoming == null ? 0f : incoming[item.id];
                if(row.stock != 0 || row.grossPerMinute != 0 || row.netPerMinute != 0 || row.exportPerMinute != 0 || row.receivedPerMinute != 0 || row.offeredPerMinute != 0) colony.materials.add(row);
            }
            colony.platformsKnown = colony.active;
            if(colony.active){
                for(var building : state.rules.defaultTeam.data().buildings){
                    if(building instanceof LaunchPadBuild pad){
                        LaunchPad type = (LaunchPad)pad.block;
                        Platform p = new Platform();p.block = type;p.position = pad.pos();p.launch = true;p.enabled = pad.enabled;
                        p.buffered = pad.items.total();p.capacity = type.itemCapacity;p.resource = pad.items.first();p.efficiency = pad.efficiency;
                        pad.items.each((item, amount) -> p.cargo.add(new ItemStack(item, amount)));
                        p.nominalPerMinute = type.launchTime > 0f ? type.itemCapacity * 3600f / type.launchTime : 0f;
                        p.charge = type.launchTime > 0 ? pad.launchCounter / type.launchTime : 0f;
                        for(var consumer : type.consumers) if(consumer instanceof ConsumeLiquid liquid){
                            p.liquidType = liquid.liquid;p.liquid = pad.liquids.get(liquid.liquid);p.liquidPerSecond = liquid.amount * 60f;
                        }
                        if(!p.enabled) p.conditions.add("disabled");
                        if(p.buffered < p.capacity) p.conditions.add("loading");
                        if(p.charge < 1f && p.efficiency <= 0f) p.conditions.add("chargeblocked");
                        if(p.charge >= 1f) p.conditions.add("charged");
                        if(colony.destination == null || colony.destination == sector || colony.destination.planet != planet || !colony.destination.hasBase()) p.conditions.add("invaliddestination");
                        colony.platforms.add(p);
                    }else if(building instanceof LandingPadBuild pad){
                        LandingPad type = (LandingPad)pad.block;
                        Platform p = new Platform();p.block = type;p.position = pad.pos();p.resource = pad.config;p.arriving = pad.arriving;
                        p.enabled = pad.enabled;p.buffered = pad.items.total();p.capacity = type.itemCapacity;p.efficiency = pad.efficiency;p.cooldown = pad.cooldown;
                        p.nominalPerMinute = type.itemCapacity * 3600f / (type.arrivalDuration + type.cooldownTime);
                        p.liquidType = type.consumeLiquid;p.liquid = pad.liquids.get(type.consumeLiquid);p.requiredLiquid = type.consumeLiquidAmount;
                        if(!p.enabled) p.conditions.add("disabled");
                        if(p.resource == null) p.conditions.add("unconfigured");
                        if(p.buffered > 0) p.conditions.add("outputoccupied");
                        if(p.cooldown > 0f) p.conditions.add("cooldown");
                        if(p.arriving == null && p.liquid < p.requiredLiquid) p.conditions.add("water");
                        Material configured = p.resource == null ? null : colony.material(p.resource);
                        if(p.resource != null && (configured == null || configured.offeredPerMinute <= 0)) p.conditions.add("nooffer");
                        if(colony.legacy) p.conditions.add("legacyreceiver");
                        colony.platforms.add(p);
                    }
                }
            }
            result.colonies.add(colony);
        }
        result.colonies.sort(c -> c.sector.id);
        return result;
    }
}
