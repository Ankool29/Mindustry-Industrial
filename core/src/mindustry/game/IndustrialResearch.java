package mindustry.game;

import arc.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Independent research bookkeeping. Gameplay remains unrestricted in mission 6B. */
public final class IndustrialResearch{
    public static final String settingsKey = "industrial-research-v1-industrial-serpulo";
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

    private static Progress progress(){
        Object payload = Core.settings.get(settingsKey, null);
        if(cachedProgress == null || cachedSettings != Core.settings || cachedPayload != payload){
            cachedSettings = Core.settings;
            cachedPayload = payload;
            cachedProgress = Core.settings.getJson(settingsKey, Progress.class, Progress::new);
        }
        return cachedProgress;
    }

    private static void save(){
        Core.settings.putJson(settingsKey, progress());
        cachedPayload = Core.settings.get(settingsKey, null);
    }

    /** No classic unlock flag or UnlockEvent is written, including when called by production. */
    public static void discover(UnlockableContent value){
        if(net.client() || !(value instanceof Item || value instanceof Liquid)) return;
        if(progress().discoveries.addUnique(key(value))) save();
    }

    public static boolean discovered(UnlockableContent value){
        return progress().discoveries.contains(key(value));
    }

    public static boolean unlocked(UnlockableContent value){
        return value.alwaysUnlocked || progress().unlocked.contains(key(value));
    }

    public static boolean unrestricted(){
        // There is deliberately no progression switch in 6B. No old save is retroactively locked.
        return true;
    }

    private static String paymentKey(UnlockableContent value, Item item){
        return key(value) + "/" + item.name;
    }

    private static int paid(UnlockableContent value, Item item){
        return Math.max(0, progress().payments.getInt(paymentKey(value, item), 0));
    }

    /** Uses the same objective interface as native research, with an explicit industrial scope. */
    public static class Researched implements Objective{
        public final UnlockableContent content;
        public Researched(UnlockableContent content){ this.content = content; }
        public boolean complete(){ return unlocked(content); }
        public String display(){ return Core.bundle.format("requirement.research", content.localizedName); }
    }

    public static class Discovered implements Objective{
        public final UnlockableContent content;
        public Discovered(UnlockableContent content){ this.content = content; }
        public boolean complete(){ return discovered(content); }
        public String display(){ return Core.bundle.format("requirement.produce", content.localizedName); }
    }

    /** Available for a future tree; no expansion milestone is imposed in 6B. */
    public static class EstablishedBases implements Objective{
        public final int count;
        public EstablishedBases(int count){
            if(count < 1) throw new IllegalArgumentException("Positive base count required");
            this.count = count;
        }
        public boolean complete(){ return Planets.industrialSerpulo.sectors.count(Sector::hasBase) >= count; }
        public String display(){ return Core.bundle.format("industrial.research.bases", count); }
    }

    private static Objective dependency(UnlockableContent value){
        return value instanceof Item || value instanceof Liquid ? new Discovered(value) : new Researched(value);
    }

    public static class IndustrialNode extends TechNode{
        private IndustrialNode(IndustrialNode parent, UnlockableContent value, ItemStack[] requirements){
            super(parent, value, requirements, false);
            planet = Planets.industrialSerpulo;
            setupRequirements(requirements);
            value.getDependencies(d -> objectives.add(dependency(d)));
        }

        @Override
        public void setupRequirements(ItemStack[] requirements){
            this.requirements = ItemStack.copy(requirements);
            finishedRequirements = ItemStack.copy(requirements);
            refresh();
        }

        public void refresh(){
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i].amount = Math.min(requirements[i].amount, paid(content, requirements[i].item));
            }
        }

        @Override
        public void save(){
            for(int i = 0; i < requirements.length; i++){
                progress().payments.put(paymentKey(content, requirements[i].item),
                    Integer.toString(Math.max(0, Math.min(requirements[i].amount, finishedRequirements[i].amount))));
            }
            IndustrialResearch.save();
        }
    }

    /** Detached preparation tree. The catalogue stays in place; no new UI or progression is activated. */
    public static class Tree{
        public final IndustrialNode root;
        public final ObjectMap<UnlockableContent, IndustrialNode> nodes = new ObjectMap<>();

        public Tree(){
            root = copy(Planets.serpulo.techTree, null);
            root.name = "industrial-serpulo";
        }

        private IndustrialNode copy(TechNode source, IndustrialNode parent){
            boolean include = !(source.content instanceof SectorPreset) && IndustrialContent.industrial(source.content);
            IndustrialNode node = include ? new IndustrialNode(parent, source.content, source.requirements) : parent;
            if(include){
                nodes.put(source.content, node);
                for(var objective : source.objectives){
                    if(objective instanceof Research r && IndustrialContent.industrial(r.content)) node.objectives.add(dependency(r.content));
                    if(objective instanceof Produce p) node.objectives.add(new Discovered(p.content));
                    // Classic SectorComplete/OnSector objectives never refer to industrial sectors.
                }
            }
            for(var child : source.children) copy(child, node);
            return node;
        }
    }

    public static Tree tree(){ return new Tree(); }

    public static ItemSeq availableItems(){
        ItemSeq result = new ItemSeq();
        for(var sector : Planets.industrialSerpulo.sectors){
            if(sector.hasBase() && !sector.isFrozen()){
                sector.items().each((item, amount) -> result.add(item, Math.max(0, amount)));
            }
        }
        return result;
    }

    /** Explicit research action for the future UI, never called by the current catalogue or simulation. */
    public static ItemSeq spend(IndustrialNode node){
        ItemSeq spent = new ItemSeq();
        if(net.client() || node.planet != Planets.industrialSerpulo || unlocked(node.content)) return spent;
        if(node.parent != null && !unlocked(node.parent.content)) return spent;
        if(node.objectives.contains(o -> !o.complete())) return spent;
        node.refresh();
        boolean complete = true;
        for(int i = 0; i < node.requirements.length; i++){
            var requirement = node.requirements[i];
            int remaining = requirement.amount - node.finishedRequirements[i].amount;
            // Same native sector withdrawal mechanism; inactive bases are used before the active base.
            for(int pass = 0; pass < 2 && remaining > 0; pass++){
                for(var sector : Planets.industrialSerpulo.sectors){
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
            progress().unlocked.addUnique(key(node.content));
            save();
        }
        return spent;
    }
}
