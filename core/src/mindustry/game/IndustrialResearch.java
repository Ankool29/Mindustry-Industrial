package mindustry.game;

import arc.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Planet-scoped industrial research; legacy worlds remain unrestricted. */
public final class IndustrialResearch{
    public static final String settingsKey = "industrial-research-v1-industrial-serpulo";
    private static Planet cachedPlanet;
    private static Settings cachedSettings;
    private static Object cachedPayload;
    private static Progress cachedProgress;

    private IndustrialResearch(){}

    public static class Progress{
        public int version = 1;
        public boolean unrestricted = true;
        public Seq<String> unlocked = new Seq<>(), discoveries = new Seq<>();
        public StringMap payments = new StringMap();
    }

    private static String key(UnlockableContent value){
        return value.getContentType().name() + ":" + value.name;
    }

    public static Planet scope(){
        return IndustrialCampaign.active() ? state.rules.sector.planet : Planets.industrialSerpulo;
    }

    public static String settingsKey(Planet planet){
        if(!IndustrialCampaign.isPlanet(planet)) throw new IllegalArgumentException("Industrial planet required");
        return "industrial-research-v1-" + planet.name;
    }

    private static Progress progress(Planet planet){
        String name = settingsKey(planet);
        Object payload = Core.settings.get(name, null);
        if(cachedProgress == null || cachedPlanet != planet || cachedSettings != Core.settings || cachedPayload != payload){
            cachedPlanet = planet;
            cachedSettings = Core.settings;
            cachedPayload = payload;
            cachedProgress = Core.settings.getJson(name, Progress.class, Progress::new);
            if(payload == null) cachedProgress.unrestricted = !IndustrialCampaign.strict(planet);
        }
        return cachedProgress;
    }

    private static void save(Planet planet){
        Core.settings.putJson(settingsKey(planet), progress(planet));
        cachedPayload = Core.settings.get(settingsKey(planet), null);
    }

    /** No classic unlock flag or UnlockEvent is written, including when called by production. */
    public static void discover(UnlockableContent value){ discover(value, scope()); }

    public static void discover(UnlockableContent value, Planet planet){
        if(net.client() || !(value instanceof Item || value instanceof Liquid)) return;
        if(progress(planet).discoveries.addUnique(key(value))) save(planet);
    }

    public static boolean discovered(UnlockableContent value){ return discovered(value, scope()); }
    public static boolean discovered(UnlockableContent value, Planet planet){
        return progress(planet).discoveries.contains(key(value));
    }

    public static boolean unlocked(UnlockableContent value){ return unlocked(value, scope()); }
    public static boolean unlocked(UnlockableContent value, Planet planet){
        if(value.alwaysUnlocked) return true;
        if(IndustrialCampaign.strict(planet)){
            if(value == Blocks.mechanicalDrill || value == Blocks.conveyor || value == UnitTypes.alpha) return true;
            if(value instanceof Item || value instanceof Liquid) return discovered(value, planet);
        }
        return progress(planet).unlocked.contains(key(value));
    }

    public static boolean unrestricted(){ return unrestricted(scope()); }
    public static boolean unrestricted(Planet planet){ return !IndustrialCampaign.strict(planet); }

    public static boolean available(UnlockableContent value, Planet planet){
        return unrestricted(planet) || (IndustrialContent.industrial(value) && unlocked(value, planet));
    }

    private static String paymentKey(UnlockableContent value, Item item){
        return key(value) + "/" + item.name;
    }

    private static int paid(UnlockableContent value, Item item, Planet planet){
        return Math.max(0, progress(planet).payments.getInt(paymentKey(value, item), 0));
    }

    /** Uses the same objective interface as native research, with an explicit industrial scope. */
    public static class Researched implements Objective{
        public final UnlockableContent content;
        public final Planet planet;
        public Researched(UnlockableContent content){ this(content, scope()); }
        public Researched(UnlockableContent content, Planet planet){ this.content = content; this.planet = planet; }
        public boolean complete(){ return unlocked(content, planet); }
        public String display(){ return Core.bundle.format("requirement.research", content.localizedName); }
    }

    public static class Discovered implements Objective{
        public final UnlockableContent content;
        public final Planet planet;
        public Discovered(UnlockableContent content){ this(content, scope()); }
        public Discovered(UnlockableContent content, Planet planet){ this.content = content; this.planet = planet; }
        public boolean complete(){ return discovered(content, planet); }
        public String display(){ return Core.bundle.format("requirement.produce", content.localizedName); }
    }

    /** Available for a future tree; no expansion milestone is imposed in 6B. */
    public static class EstablishedBases implements Objective{
        public final int count;
        public final Planet planet;
        public EstablishedBases(int count){ this(scope(), count); }
        public EstablishedBases(Planet planet, int count){
            if(count < 1) throw new IllegalArgumentException("Positive base count required");
            this.count = count;
            settingsKey(planet);
            this.planet = planet;
        }
        public boolean complete(){ return planet.sectors.count(Sector::hasBase) >= count; }
        public String display(){ return Core.bundle.format("industrial.research.bases", count); }
    }

    private static void addDependency(IndustrialNode node, UnlockableContent value){
        boolean present = node.objectives.contains(o -> (o instanceof Discovered d && d.content == value) || (o instanceof Researched r && r.content == value));
        if(!present) node.objectives.add(dependency(value, node.planet));
    }

    private static Objective dependency(UnlockableContent value, Planet planet){
        return value instanceof Item || value instanceof Liquid ? new Discovered(value, planet) : new Researched(value, planet);
    }

    public static class IndustrialNode extends TechNode{
        private IndustrialNode(IndustrialNode parent, UnlockableContent value, ItemStack[] requirements, Planet scope){
            super(parent, value, requirements, false);
            planet = scope;
            setupRequirements(requirements);
            value.getDependencies(d -> addDependency(this, d));
        }

        @Override
        public void setupRequirements(ItemStack[] requirements){
            this.requirements = ItemStack.copy(requirements);
            finishedRequirements = ItemStack.copy(requirements);
            refresh();
        }

        public void refresh(){
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i].amount = Math.min(requirements[i].amount, paid(content, requirements[i].item, planet));
            }
        }

        @Override
        public void save(){
            for(int i = 0; i < requirements.length; i++){
                progress(planet).payments.put(paymentKey(content, requirements[i].item),
                    Integer.toString(Math.max(0, Math.min(requirements[i].amount, finishedRequirements[i].amount))));
            }
            IndustrialResearch.save(planet);
        }
    }

    /** Detached preparation tree. The catalogue stays in place; no new UI or progression is activated. */
    public static class Tree{
        public final IndustrialNode root;
        public final ObjectMap<UnlockableContent, IndustrialNode> nodes = new ObjectMap<>();

        public final Planet planet;
        public Tree(){ this(scope()); }
        public Tree(Planet planet){
            settingsKey(planet);
            this.planet = planet;
            root = copy(Planets.serpulo.techTree, null);
            root.name = planet.name;
            if(IndustrialCampaign.strict(planet)) organize();
        }

        private void parent(UnlockableContent value, UnlockableContent parent){
            IndustrialNode node = nodes.get(value), target = nodes.get(parent);
            if(node == null || target == null || node == root) return;
            if(node.parent != null) node.parent.children.remove(node);
            node.parent = target;
            target.children.add(node);
        }

        private void organize(){
            parent(Blocks.graphitePress, Blocks.coreShard);
            parent(Blocks.combustionGenerator, Blocks.coreShard);
            parent(Blocks.microProcessor, Blocks.coreShard);
            parent(Blocks.pneumaticDrill, Blocks.mechanicalDrill);
            nodes.get(Blocks.pneumaticDrill).objectives.add(new Researched(Blocks.graphitePress, planet));
            parent(Blocks.siliconSmelter, Blocks.graphitePress);
            parent(Blocks.kiln, Blocks.graphitePress);
            parent(Blocks.pulverizer, Blocks.combustionGenerator);
            parent(Blocks.mechanicalPump, Blocks.kiln);
            parent(Blocks.waterExtractor, Blocks.kiln);
            parent(Blocks.cultivator, Blocks.siliconSmelter);
            parent(Blocks.sporePress, Blocks.cultivator);
            parent(Blocks.cryofluidMixer, Blocks.mechanicalPump);
            parent(Blocks.surgeSmelter, Blocks.siliconSmelter);
            parent(Blocks.phaseWeaver, Blocks.siliconSmelter);
            parent(Blocks.airFactory, Blocks.siliconSmelter);
            parent(Blocks.groundFactory, Blocks.siliconSmelter);
            parent(Blocks.navalFactory, Blocks.kiln);
            parent(Blocks.additiveReconstructor, Blocks.airFactory);
            root.each(n -> { n.rootNode = n == root ? null : root; n.depth = n.parent == null ? 0 : n.parent.depth + 1; });
        }

        private IndustrialNode copy(TechNode source, IndustrialNode parent){
            boolean include = !(source.content instanceof SectorPreset) && IndustrialContent.industrial(source.content);
            IndustrialNode node = include ? new IndustrialNode(parent, source.content, source.requirements, planet) : parent;
            if(include){
                nodes.put(source.content, node);
                for(var objective : source.objectives){
                    if(objective instanceof Research r && IndustrialContent.industrial(r.content)) addDependency(node, r.content);
                    if(objective instanceof Produce p) addDependency(node, p.content);
                    // Classic SectorComplete/OnSector objectives never refer to industrial sectors.
                }
            }
            for(var child : source.children) copy(child, node);
            return node;
        }
    }

    public static Tree tree(){ return new Tree(); }
    public static Tree tree(Planet planet){ return new Tree(planet); }

    public static ItemSeq availableItems(){ return availableItems(scope()); }
    public static ItemSeq availableItems(Planet planet){
        settingsKey(planet);
        ItemSeq result = new ItemSeq();
        for(var sector : planet.sectors){
            if(sector.hasBase() && !sector.isFrozen()){
                sector.items().each((item, amount) -> result.add(item, Math.max(0, amount)));
            }
        }
        return result;
    }

    public static boolean canResearch(IndustrialNode node){
        return IndustrialCampaign.isPlanet(node.planet) && !unlocked(node.content, node.planet) &&
            (node.parent == null || unlocked(node.parent.content, node.planet)) && !node.objectives.contains(o -> !o.complete());
    }

    /** Explicit research action; never called automatically by the catalogue or simulation. */
    public static ItemSeq spend(IndustrialNode node){
        ItemSeq spent = new ItemSeq();
        if(net.client() || !canResearch(node)) return spent;
        Planet planet = node.planet;
        node.refresh();
        boolean complete = true;
        for(int i = 0; i < node.requirements.length; i++){
            var requirement = node.requirements[i];
            int remaining = requirement.amount - node.finishedRequirements[i].amount;
            // Same native sector withdrawal mechanism; inactive bases are used before the active base.
            for(int pass = 0; pass < 2 && remaining > 0; pass++){
                for(var sector : planet.sectors){
                    if(!sector.hasBase() || sector.isFrozen() || sector.isBeingPlayed() != (pass == 1)) continue;
                    int used = Math.min(remaining, Math.max(0, sector.items().get(requirement.item)));
                    if(used <= 0) continue;
                    sector.removeItem(requirement.item, used);
                    remaining -= used;
                    node.finishedRequirements[i].amount += used;
                    spent.add(requirement.item, used);
                }
            }
            if(remaining > 0) complete = false;
        }
        node.save();
        if(complete){
            progress(planet).unlocked.addUnique(key(node.content));
            save(planet);
        }
        return spent;
    }
}
