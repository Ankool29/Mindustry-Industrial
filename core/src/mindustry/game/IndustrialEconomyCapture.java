package mindustry.game;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.campaign.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import mindustry.world.blocks.distribution.Conveyor.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.power.PowerGenerator.*;
import mindustry.world.blocks.power.ConsumeGenerator.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.Drill.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.blocks.production.Pump.*;
import mindustry.world.blocks.production.SolidPump.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.blocks.storage.StorageBlock.*;
import mindustry.world.blocks.storage.Unloader.*;
import mindustry.world.consumers.*;
import mindustry.world.modules.*;

import java.security.*;
import java.nio.charset.*;
import java.util.*;

import static mindustry.Vars.*;

/** Session-only observational DTOs. No native object, module or economic authority is retained in a snapshot.
 * Capture runs synchronously at a load/checkpoint or explicit inspection, never from a simulation tick.
 */
public final class IndustrialEconomyCapture{
    public static final int maxNodes = 10000, maxLinks = 80000, maxSnapshots = 100, maxCachedNodes = 100000;
    private static final Seq<Snapshot> cache = new Seq<>();
    private static Settings session;
    private IndustrialEconomyCapture(){}

    public enum Origin{load, checkpoint, manual}
    public enum Kind{adjacency, items, liquid, power}
    public static class ItemStore{
        public int owner;
        public IntSeq members = new IntSeq(), ids = new IntSeq(), amounts = new IntSeq();
    }
    public static class LiquidStore{
        public int owner;
        public IntSeq members = new IntSeq(), ids = new IntSeq();
        public FloatSeq amounts = new FloatSeq();
    }
    public static class PowerStore{
        public int owner;
        public IntSeq members = new IntSeq();
        public float status, capacity;
        public boolean buffered;
    }
    public static class Circuit{
        public IntSeq members = new IntSeq();
        public float observedInPerTick, observedOutPerTick;
        public double energy;
    }
    public static class Consumer{
        public String type, liquid;
        public IntSeq itemIds = new IntSeq(), amounts = new IntSeq();
        public float liquidAmount, liquidPerTick, powerPerTick, powerCapacity;
        public boolean optional, booster, continuous, buffered, nativeUpdate, known;
    }
    public static class Spec{
        public String name, type;
        public int itemCapacity;
        public float liquidCapacity, cycleTicks, hardnessMultiplier, boostMultiplier, warmupSpeed;
        public int drillTier;
        public boolean supported;
        public Seq<Consumer> consumers = new Seq<>();
        public IntSeq outputItems = new IntSeq(), outputAmounts = new IntSeq();
        public Seq<String> outputLiquids = new Seq<>();
        public FloatSeq liquidAmounts = new FloatSeq();
    }
    public static class Node{
        public int position, rotation, spec, itemStore = -1, liquidStore = -1, powerStore = -1, circuit = -1;
        public int coreCapacity;
        public boolean enabled, stateDecoded;
        public float efficiency, optionalEfficiency, timeScale;
        public StringMap tags = new StringMap();
        public ObjectFloatMap<String> phase = new ObjectFloatMap<>();
        public int[] conveyorIds = {};
        public float[] conveyorX = {}, conveyorY = {};
    }
    public static class Link{
        public int from, to;
        public Kind kind;
        /** A supported structural route, not an observed flow or promise of current availability. */
        public boolean supported;
    }
    public static class LooseCargo{
        public int nativeId;
        public String kind;
        public int target = -1;
        public float progress;
        public IntSeq ids = new IntSeq(), amounts = new IntSeq();
    }
    public static class Issue{
        public String code, detail, block;
        public boolean blocking;
        public int count;
        public IntSeq examples = new IntSeq();
    }
    public static class Snapshot{
        public String planet, generation, topology;
        public int sector, nativeBuildings, team, destination = -1;
        public String destinationPlanet;
        public Origin origin;
        public long millis, nanos, captureNanos;
        public double worldTick;
        public boolean complete = true, stocksComplete = true, candidate, coreIncinerates, infiniteResources;
        public String[] itemNames, liquidNames;
        public long[] itemTotals;
        public double[] liquidTotals;
        public double energy;
        public Seq<Node> nodes = new Seq<>();
        public Seq<Spec> specs = new Seq<>();
        public Seq<ItemStore> itemStores = new Seq<>();
        public Seq<LiquidStore> liquidStores = new Seq<>();
        public Seq<PowerStore> powerStores = new Seq<>();
        public Seq<Circuit> circuits = new Seq<>();
        public Seq<Link> links = new Seq<>();
        public Seq<LooseCargo> loose = new Seq<>();
        public Seq<IndustrialTransports.ManifestView> manifests = new Seq<>();
        public Seq<Issue> issues = new Seq<>();
        public int blockers(){return issues.count(i -> i.blocking);}
    }

    public static boolean enabled(Planet planet){return planet != null && planet == Planets.industrialSerpuloProgressive;}
    private static boolean worldPresent(Sector sector){return sector != null && enabled(sector.planet) && state.isCampaign() && state.getSector() == sector && !net.client();}
    public static boolean current(Sector sector){return state.isGame() && worldPresent(sector);}
    private static void session(){if(session != Core.settings){cache.clear();session = Core.settings;}}
    public static Snapshot latest(Sector sector){
        if(sector == null || !enabled(sector.planet)) return null;
        session();return cache.find(s -> s.sector == sector.id && s.planet.equals(sector.planet.name));
    }
    public static void forget(){session();cache.clear();}
    public static int cachedCount(){session();return cache.size;}
    public static int cachedNodes(){session();return cache.sum(s -> s.nodes.size);}

    /** Only observational, including on failure. Never route a diagnostic error into native save recovery. */
    public static Snapshot observe(Origin origin){
        Sector sector = state.rules.sector;
        if(!worldPresent(sector) || (origin == Origin.manual && !state.isGame())) return null;
        session();
        try{
            Snapshot s = capture(origin);
            cache.removeAll(c -> c.sector == s.sector && c.planet.equals(s.planet));cache.add(s);
            while(cache.size > maxSnapshots || cachedNodes() > maxCachedNodes) cache.remove(0);
            return s;
        }catch(Exception e){
            Log.err("Industrial graph capture unavailable (no game state changed).", e);return null;
        }
    }
    public static void saved(Fi file){
        Sector sector = state.rules.sector;
        if(worldPresent(sector) && sector.save != null && sector.save.file.equals(file)) observe(Origin.checkpoint);
    }
    public static void loaded(){if(worldPresent(state.rules.sector)) observe(Origin.load);}

    private static void issue(Snapshot s, String code, String detail, String block, int pos, boolean blocking){
        Issue i = s.issues.find(v -> v.code.equals(code) && Objects.equals(v.block, block));
        if(i == null){i = new Issue();i.code = code;i.detail = detail;i.block = block;i.blocking = blocking;s.issues.add(i);}
        i.count++;if(i.examples.size < 3) i.examples.add(pos);
    }
    private static boolean inert(Block b){return !b.update && !b.hasItems && !b.hasLiquids && !b.hasPower && !b.acceptsPayload && !b.outputsPayload;}
    private static boolean supported(Block b){
        return inert(b) || b == Blocks.coreShard || b == Blocks.mechanicalDrill || b == Blocks.unloader || b == Blocks.advancedLaunchPad ||
            b == Blocks.landingPad || b == Blocks.liquidTank || b == Blocks.powerNode || b == Blocks.batteryLarge;
    }
    private static int spec(Snapshot s, Block b){
        for(int i = 0; i < s.specs.size; i++)if(s.specs.get(i).name.equals(b.name)) return i;
        Spec p = new Spec();p.name = b.name;p.type = b.getClass().getSimpleName();if(p.type.isEmpty())p.type = b.getClass().getSuperclass().getSimpleName();p.supported = supported(b) && b.minfo.mod == null;
        p.itemCapacity = b.itemCapacity;p.liquidCapacity = b.liquidCapacity;
        for(var consumer : b.consumers){
            Consumer c = new Consumer();c.type = consumer.getClass().getSimpleName();if(c.type.isEmpty())c.type = consumer.getClass().getSuperclass().getSimpleName();
            c.optional = consumer.optional;c.booster = consumer.booster;c.nativeUpdate = consumer.update;
            c.known = consumer.getClass() == ConsumeItems.class || consumer.getClass() == ConsumeLiquid.class || consumer.getClass() == ConsumePower.class || (b == Blocks.landingPad && consumer instanceof ConsumeLiquid);
            if(!c.known)p.supported = false;
            if(consumer instanceof ConsumeItems items){for(var item : items.items){c.itemIds.add(item.item.id);c.amounts.add(item.amount);}}
            else if(consumer instanceof ConsumeLiquid liquid){c.liquid = liquid.liquid.name;c.liquidAmount = liquid.amount;c.continuous = liquid.update;if(c.continuous)c.liquidPerTick = liquid.amount;}
            else if(consumer instanceof ConsumePower power){c.powerPerTick = power.usage;c.powerCapacity = power.capacity;c.buffered = power.buffered;c.continuous = !power.buffered;}
            else p.supported = false;
            p.consumers.add(c);
        }
        if(b instanceof GenericCrafter crafter){
            p.cycleTicks = crafter.craftTime;
            if(crafter.outputItems != null)for(var output : crafter.outputItems){p.outputItems.add(output.item.id);p.outputAmounts.add(output.amount);}
            if(crafter.outputLiquids != null)for(var output : crafter.outputLiquids){p.outputLiquids.add(output.liquid.name);p.liquidAmounts.add(output.amount);}
        }else if(b instanceof LaunchPad pad) p.cycleTicks = pad.launchTime;
        else if(b instanceof LandingPad pad) p.cycleTicks = pad.arrivalDuration;
        else if(b instanceof Drill drill){p.cycleTicks = drill.drillTime;p.drillTier = drill.tier;p.hardnessMultiplier = drill.hardnessDrillMultiplier;p.boostMultiplier = drill.liquidBoostIntensity;p.warmupSpeed = drill.warmupSpeed;}
        s.specs.add(p);return s.specs.size - 1;
    }
    private static Snapshot capture(Origin origin) throws NoSuchAlgorithmException{
        long start = Time.nanos();Snapshot s = new Snapshot();Sector sector = state.getSector();
        s.planet = sector.planet.name;s.sector = sector.id;s.team = state.rules.defaultTeam.id;
        if(sector.info.destination != null){s.destination = sector.info.destination.id;s.destinationPlanet = sector.info.destination.planet.name;}
        s.generation = state.rules.tags.get("industrial-transport-generation-v1", "M7B/no-marker");
        s.origin = origin;s.worldTick = state.tick;s.millis = Time.millis();s.nanos = start;
        s.itemNames = new String[content.items().size];s.itemTotals = new long[s.itemNames.length];
        s.liquidNames = new String[content.liquids().size];s.liquidTotals = new double[s.liquidNames.length];
        for(var item : content.items())s.itemNames[item.id] = item.name;
        for(var liquid : content.liquids())s.liquidNames[liquid.id] = liquid.name;
        s.infiniteResources = state.rules.infiniteResources;s.coreIncinerates = state.rules.coreIncinerates;
        if(state.rules.infiniteResources) issue(s, "infinite", "Règles à ressources infinies hors périmètre", null, -1, true);
        if(state.rules.coreIncinerates) issue(s, "incineration", "Incinération du noyau hors prototype minimal", null, -1, true);
        Seq<Building> buildings = new Seq<>();
        for(var b : state.rules.defaultTeam.data().buildings){
            s.nativeBuildings++;
            if(buildings.size < maxNodes && b.tile != null) buildings.add(b);
        }
        if(s.nativeBuildings > buildings.size){s.complete = s.stocksComplete = false;issue(s, "limit", "Capture tronquée : limite de bâtiments", null, -1, true);}
        buildings.sort((a, b) -> Integer.compare(a.pos(), b.pos()));
        var nodeIds = new IdentityHashMap<Building, Integer>();
        var items = new IdentityHashMap<ItemModule, Integer>();var liquids = new IdentityHashMap<LiquidModule, Integer>();
        var powers = new IdentityHashMap<PowerModule, Integer>();var circuits = new IdentityHashMap<PowerGraph, Integer>();
        for(int index = 0; index < buildings.size; index++){
            Building b = buildings.get(index);Node n = new Node();n.position = b.pos();n.rotation = b.rotation;n.enabled = b.enabled;
            n.efficiency = b.efficiency;n.optionalEfficiency = b.optionalEfficiency;n.timeScale = b.timeScale();n.spec = spec(s, b.block);
            s.nodes.add(n);nodeIds.put(b, index);
            if(!s.specs.get(n.spec).supported) issue(s, "unsupported", "Équipement/consommateur non pris en charge par le prototype M7D-A", b.block.name, n.position, true);
            if(!b.enabled) issue(s, "disabled", "Équipement désactivé (état observé)", b.block.name, n.position, false);
            if(!Float.isFinite(n.efficiency) || !Float.isFinite(n.timeScale)){s.complete = false;issue(s, "invalid", "État numérique invalide", b.block.name, n.position, true);}
            if(Math.abs(n.timeScale - 1f) > 0.0001f)issue(s, "overdrive", "Surcadence non prise en charge", b.block.name, n.position, true);
            if(b.items != null){
                Integer id = items.get(b.items);
                if(id == null){
                    id = s.itemStores.size;items.put(b.items, id);ItemStore store = new ItemStore();store.owner = index;
                    for(int i = 0; i < b.items.length(); i++){
                        int count = b.items.get(i);if(count == 0)continue;
                        store.ids.add(i);store.amounts.add(count);
                        if(i >= s.itemTotals.length || count < 0){s.stocksComplete = s.complete = false;issue(s, "invalid-items", "Inventaire invalide/inconnu, aucune correction appliquée", b.block.name, n.position, true);}
                        if(i < s.itemTotals.length)s.itemTotals[i] += count;
                    }
                    s.itemStores.add(store);
                }
                n.itemStore = id;s.itemStores.get(id).members.add(index);
            }
            if(b.liquids != null){
                Integer id = liquids.get(b.liquids);
                if(id == null){
                    id = s.liquidStores.size;liquids.put(b.liquids, id);LiquidStore store = new LiquidStore();store.owner = index;
                    for(var liquid : content.liquids()){
                        float amount = b.liquids.get(liquid);if(amount == 0f)continue;
                        store.ids.add(liquid.id);store.amounts.add(amount);s.liquidTotals[liquid.id] += amount;
                        if(!Float.isFinite(amount) || amount < 0){s.stocksComplete = s.complete = false;issue(s, "invalid-liquid", "Volume liquide invalide", b.block.name, n.position, true);}
                    }
                    s.liquidStores.add(store);
                }
                n.liquidStore = id;s.liquidStores.get(id).members.add(index);
            }
            if(b.power != null){
                Integer id = powers.get(b.power);
                boolean buffered = b.block.consPower != null && b.block.consPower.buffered;
                float capacity = buffered ? b.block.consPower.capacity : 0f;
                if(id == null){
                    id = s.powerStores.size;powers.put(b.power, id);PowerStore store = new PowerStore();store.owner = index;
                    store.status = b.power.status;store.buffered = buffered;store.capacity = capacity;s.powerStores.add(store);
                    if(buffered)s.energy += (double)store.status * capacity;
                    if(!Float.isFinite(store.status) || store.status < 0 || store.status > 1.0001f){s.complete = false;issue(s, "invalid-power", "État électrique invalide", b.block.name, n.position, true);}
                }else if(s.powerStores.get(id).buffered != buffered || s.powerStores.get(id).capacity != capacity){
                    s.stocksComplete = s.complete = false;issue(s, "power-alias", "Module électrique partagé avec capacités contradictoires", b.block.name, n.position, true);
                }
                n.powerStore = id;s.powerStores.get(id).members.add(index);
                PowerGraph graph = b.power.graph;Integer g = circuits.get(graph);
                if(g == null){
                    g = s.circuits.size;circuits.put(graph, g);Circuit circuit = new Circuit();
                    if(graph.getClass() == PowerGraph.class){circuit.observedInPerTick = graph.getLastScaledPowerIn();circuit.observedOutPerTick = graph.getLastScaledPowerOut();}
                    else issue(s, "custom-power", "Réseau électrique personnalisé non pris en charge", b.block.name, n.position, true);
                    s.circuits.add(circuit);
                }
                n.circuit = g;s.circuits.get(g).members.add(index);
                if(buffered && s.powerStores.get(id).owner == index)s.circuits.get(g).energy += (double)b.power.status * capacity;
            }
            if(b.liquids != null && (b.block == Blocks.liquidTank || b.block == Blocks.advancedLaunchPad || b.block == Blocks.landingPad)){
                Liquid expected = b.block == Blocks.landingPad ? Liquids.water : Liquids.oil;
                for(var liquid : content.liquids())if(liquid != expected && b.liquids.get(liquid) > 0)issue(s, "liquid-config", "Liquide hors périmètre du prototype", b.block.name, b.pos(), true);
            }
            if(b.block.minfo.mod != null){s.complete = s.stocksComplete = false;issue(s, "mod-state", "Contenu modifié : états internes non garantis", b.block.name, b.pos(), true);}
            if(b.block.outputsPayload || b.block.acceptsPayload){s.complete = s.stocksComplete = false;issue(s, "payload-block", "Payloads internes du bâtiment non décodés", b.block.name, b.pos(), true);}
            decode(s, n, b);
        }
        for(var entry : circuits.entrySet()){
            PowerGraph graph = entry.getKey();Circuit c = s.circuits.get(entry.getValue());
            if(graph.all.isEmpty() || graph.all.contains(b -> !nodeIds.containsKey(b))){s.complete = false;issue(s, "power-scope", "Réseau électrique incomplet ou non initialisé", null, -1, true);}
        }
        if(buildings.count(b -> b instanceof CoreBuild) > 1)issue(s, "multi-core", "Plusieurs noyaux hors prototype initial ; stock partagé capturé", null, -1, true);
        if(!buildings.contains(b -> b instanceof CoreBuild))issue(s, "no-core", "Aucun noyau présent dans le relevé", null, -1, true);
        Set<Long> seen = new HashSet<>();
        for(int i = 0; i < buildings.size; i++){
            Building b = buildings.get(i);
            for(var other : b.proximity){
                Integer j = nodeIds.get(other);if(j == null)continue;
                link(s, seen, i, j, Kind.adjacency, false);
                boolean route = (b instanceof DrillBuild || b instanceof LandingPadBuild) && other instanceof CoreBuild ||
                    b instanceof CoreBuild && other instanceof UnloaderBuild || b instanceof UnloaderBuild && other instanceof LaunchPadBuild;
                if(route){
                    boolean recognized = s.specs.get(s.nodes.get(i).spec).supported && s.specs.get(s.nodes.get(j).spec).supported;
                    link(s, seen, i, j, Kind.items, recognized);
                    if(!recognized)issue(s, "unverified-route", "Liaison locale hors périmètre : proximité n'est pas preuve d'un débit", b.block.name, b.pos(), true);
                }
                if(b.block == Blocks.liquidTank && other.liquids != null){
                    boolean supported = other.block == Blocks.advancedLaunchPad;
                    link(s, seen, i, j, Kind.liquid, supported);
                    if(!supported)issue(s, "liquid-route", "Connexion liquide hors prototype minimal", b.block.name, b.pos(), true);
                }
                if(b.power != null && other.power != null && b.power.graph == other.power.graph)link(s, seen, i, j, Kind.power, true);
            }
            if(b.power != null)for(int pos : b.power.links.toArray()){
                Building other = world.build(pos);Integer j = nodeIds.get(other);
                if(j != null)link(s, seen, i, j, Kind.power, other.power != null && other.power.graph == b.power.graph);
                else{s.complete = false;issue(s, "external-power", "Lien électrique hors relevé", b.block.name, b.pos(), true);}
            }
            if(b instanceof UnloaderBuild){
                int sources = b.proximity.count(p -> p instanceof CoreBuild), targets = b.proximity.count(p -> p instanceof LaunchPadBuild);
                if(sources > 1 || targets > 1 || b.proximity.contains(p -> p.items != null && !(p instanceof CoreBuild) && !(p instanceof LaunchPadBuild)))
                    issue(s, "unloader-routing", "Voisinage du déchargeur hors liaison simple noyau/rampe", b.block.name, b.pos(), true);
                else if(sources == 0 || targets == 0)issue(s, "unloader-disconnected", "Déchargeur sans liaison complète noyau/rampe", b.block.name, b.pos(), false);
            }
            if(b instanceof ConveyorBuild){
                Building front = b.front();Integer j = nodeIds.get(front);
                if(j != null)link(s, seen, i, j, Kind.items, false);
            }
        }
        // Native unit cargo is physical. Payload interiors remain explicitly incomplete, never guessed.
        int units = 0;
        // TeamData.units is rebuilt by updateTeamStats, so it may lag immediately after a load/add.
        for(var unit : Groups.unit){
            if(unit.team != state.rules.defaultTeam)continue;
            if(++units > maxNodes){s.complete = s.stocksComplete = false;issue(s, "unit-limit", "Unités tronquées", null, -1, true);break;}
            if(unit.stack != null && unit.stack.item != null && unit.stack.amount > 0){
                LooseCargo cargo = new LooseCargo();cargo.kind = "unit";cargo.nativeId = unit.id;cargo.ids.add(unit.stack.item.id);cargo.amounts.add(unit.stack.amount);s.loose.add(cargo);s.itemTotals[unit.stack.item.id] += unit.stack.amount;
                issue(s, "unit-cargo", "Transport d'objets par unité non pris en charge", null, -1, true);
            }
            if(unit.mineTile != null){
                LooseCargo actor = new LooseCargo();actor.kind = "unit-mining";actor.nativeId = unit.id;actor.target = unit.mineTile.pos();actor.progress = unit.mineTimer;s.loose.add(actor);
                issue(s, "unit-mining", "Extraction par unité non prise en charge", null, actor.target, true);
            }
            if(unit instanceof Payloadc carrier && !carrier.payloads().isEmpty()){
                s.complete = s.stocksComplete = false;issue(s, "payload", "Intérieurs de payloads non décodés : inventaire partiel", null, -1, true);
            }
        }
        int flights = 0;
        for(var entity : Groups.all){
            if(entity instanceof LaunchPayload payload && payload.team == state.rules.defaultTeam && !payload.stacks.isEmpty()){
                if(++flights > 512){s.complete = s.stocksComplete = false;issue(s, "flight-limit", "Vols natifs tronqués", null, -1, true);break;}
                LooseCargo cargo = new LooseCargo();cargo.kind = "native-flight";cargo.nativeId = payload.id;
                for(var stack : payload.stacks){cargo.ids.add(stack.item.id);cargo.amounts.add(stack.amount);s.itemTotals[stack.item.id] += stack.amount;}
                s.loose.add(cargo);issue(s, "legacy-flight", "Colis natif préexistant, adoption M7C.1 non encore observée", null, -1, true);
            }
        }
        s.manifests = IndustrialTransports.inspectManifests(sector);
        s.candidate = s.complete && s.issues.count(i -> i.blocking) == 0;
        s.links.sort((a, b) -> a.from != b.from ? Integer.compare(a.from, b.from) : a.to != b.to ? Integer.compare(a.to, b.to) : a.kind.compareTo(b.kind));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update((s.planet + ":" + s.sector + ":" + s.generation + ":" + s.destinationPlanet + ":" + s.destination).getBytes(StandardCharsets.UTF_8));
        for(Node n : s.nodes){
            digest.update((n.position + ":" + s.specs.get(n.spec).name + ":" + n.rotation + ":" + n.enabled + ":" + n.itemStore + ":" + n.liquidStore + ":" + n.powerStore + ":" + n.circuit + ":" + n.tags.get("config", "") + ":" + n.tags.get("ore", "")).getBytes(StandardCharsets.UTF_8));
        }
        digest.update(JsonIO.write(s.specs).getBytes(StandardCharsets.UTF_8));
        for(Link l : s.links)digest.update((l.from + ":" + l.to + ":" + l.kind).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(64);
        for(byte b : digest.digest()){hex.append(Character.forDigit((b >>> 4) & 15, 16));hex.append(Character.forDigit(b & 15, 16));}
        s.topology = hex.toString();
        s.captureNanos = Time.nanos() - start;return s;
    }
    private static void link(Snapshot s, Set<Long> seen, int from, int to, Kind kind, boolean supported){
        if(from == to || !seen.add((((long)from * maxNodes + to) << 3) | kind.ordinal())) return;
        if(s.links.size >= maxLinks){s.complete = false;issue(s, "link-limit", "Liaisons tronquées", null, -1, true);return;}
        Link l = new Link();l.from = from;l.to = to;l.kind = kind;l.supported = supported;s.links.add(l);
    }
    private static void decode(Snapshot s, Node n, Building b){
        n.stateDecoded = true;
        if(b instanceof CoreBuild core)n.coreCapacity = core.storageCapacity;
        else if(b instanceof LaunchPadBuild pad)n.phase.put("launchCounter", pad.launchCounter);
        else if(b instanceof LandingPadBuild pad){
            n.tags.put("config", pad.config == null ? "" : pad.config.name);n.tags.put("arrival", pad.arriving == null ? "" : pad.arriving.name);n.tags.put("manifest", pad.transportId == null ? "" : pad.transportId);
            if(pad.config == null)issue(s, "receiver-config", "Réception non configurée (état observé)", b.block.name, b.pos(), false);
            else if(pad.config != Items.copper)issue(s, "receiver-item", "Prototype initial limité au cuivre", b.block.name, b.pos(), true);
            n.phase.put("arrivalProgress", pad.arrivingTimer);n.phase.put("liquidRemoved", pad.liquidRemoved);n.phase.put("cooldown", pad.cooldown);
        }else if(b instanceof StorageBuild){ /* Pure storage has no hidden process progression. */
        }else if(b instanceof DrillBuild drill){
            n.tags.put("ore", drill.dominantItem == null ? "" : drill.dominantItem.name);n.phase.put("oreTiles", drill.dominantItems);n.phase.put("progress", drill.progress);n.phase.put("warmup", drill.warmup);
            if(drill.dominantItem != Items.copper)issue(s, "ore", "Prototype limité au cuivre réellement couvert", b.block.name, b.pos(), true);
            if(drill.liquids != null && drill.liquids.get(Liquids.water) > 0)issue(s, "boost", "Boost liquide de foreuse hors prototype minimal", b.block.name, b.pos(), true);
        }else if(b instanceof GenericCrafterBuild crafter){
            n.phase.put("progress", crafter.progress);n.phase.put("warmup", crafter.warmup);n.phase.put("totalProgress", crafter.totalProgress);
            if(crafter.outputAccumulator != null)for(int i = 0; i < crafter.outputAccumulator.length; i++)n.phase.put("outputRemainder" + i, crafter.outputAccumulator[i]);
        }else if(b instanceof ConveyorBuild conveyor){
            int len = Math.min(conveyor.len, Math.min(conveyor.ids.length, Math.min(conveyor.xs.length, conveyor.ys.length)));
            n.conveyorIds = new int[len];n.conveyorX = Arrays.copyOf(conveyor.xs, len);n.conveyorY = Arrays.copyOf(conveyor.ys, len);
            int[] counts = new int[s.itemNames.length];
            for(int i = 0; i < len; i++){n.conveyorIds[i] = conveyor.ids[i] == null ? -1 : conveyor.ids[i].id;if(n.conveyorIds[i] >= 0)counts[n.conveyorIds[i]]++;}
            boolean valid = conveyor.len == len;
            for(int i = 0; i < counts.length; i++)if(counts[i] != conveyor.items.get(i))valid = false;
            if(!valid){s.complete = s.stocksComplete = false;issue(s, "conveyor-mismatch", "Représentations du convoyeur incohérentes, aucune réparation", b.block.name, b.pos(), true);}
        }else if(b instanceof UnloaderBuild unloader){
            n.tags.put("config", unloader.sortItem == null ? "" : unloader.sortItem.name);n.phase.put("unloadTimer", unloader.unloadTimer);n.phase.put("cursor", unloader.rotations);
            if(unloader.sortItem != Items.copper)issue(s, "unloader-config", "Déchargeur du prototype : filtre cuivre explicite requis", b.block.name, b.pos(), true);
        }else if(b instanceof SolidPumpBuild pump){n.phase.put("warmup", pump.warmup);n.phase.put("progress", pump.pumpTime);n.phase.put("validTiles", pump.validTiles);}
        else if(b instanceof PumpBuild pump){n.phase.put("warmup", pump.warmup);n.phase.put("progress", pump.totalProgress);n.phase.put("consumeTimer", pump.consTimer);}
        else if(b instanceof GeneratorBuild generator){
            n.phase.put("productionEfficiency", generator.productionEfficiency);n.phase.put("generateTime", generator.generateTime);
            if(b instanceof ConsumeGeneratorBuild fuel)n.phase.put("warmup", fuel.warmup);
        }else if(!inert(b.block) && !(b.block instanceof Battery) && !(b.block instanceof PowerNode) && !(b.block instanceof mindustry.world.blocks.liquid.LiquidRouter)){
            n.stateDecoded = false;s.complete = s.stocksComplete = false;issue(s, "opaque-state", "État spécifique non décodé ; modules physiques seuls", b.block.name, b.pos(), true);
        }
    }
}
