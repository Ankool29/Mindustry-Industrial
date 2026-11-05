import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.IndustrialResearch.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialProgressionTests{
    Settings previous;
    Fi directory;
    Saves saves;
    ObjectMap<Sector, SectorInfo> infos = new ObjectMap<>();
    ObjectMap<Sector, Saves.SaveSlot> slots = new ObjectMap<>();
    Planet strict;

    @BeforeAll
    static void init(){ ApplicationTests.launchApplication(); }

    @BeforeEach
    void setup(){
        logic.reset(); Time.setDeltaProvider(() -> 1f);
        previous = Core.settings; directory = previous.getDataDirectory().child("m6c-test"); directory.deleteDirectory();
        Core.settings = settings();
        strict = Planets.industrialSerpuloProgressive;
        if(Core.assets == null) Core.assets = new AssetManager();
        saves = new Saves();
        for(var planet : new Planet[]{strict, Planets.industrialSerpulo, Planets.serpulo}) for(var sector : planet.sectors){
            infos.put(sector, sector.info); slots.put(sector, sector.save);
            sector.info = new SectorInfo(); sector.save = null; IndustrialCampaign.prepareInfo(sector);
        }
        universe.clearLoadoutInfo();
    }

    Settings settings(){
        Settings result = new Settings(); result.setJson(JsonIO.json); result.setDataDirectory(directory); result.load(); return result;
    }

    @AfterEach
    void clean(){
        logic.reset(); infos.each((s,i) -> s.info=i); slots.each((s,i) -> s.save=i); Core.settings=previous; directory.deleteDirectory();
    }

    Sector base(Planet planet, int id, int copper){
        var s=planet.sectors.get(id); s.save=saves.new SaveSlot(directory.child(planet.name+id+".msav")); s.info.hasCore=true;
        s.info.storageCapacity=4000; s.info.items.set(Items.copper,copper); return s;
    }

    void arena(){
        world.loadGenerator(80,80,tiles -> {
            tiles.fill();
            for(var tile : tiles) tile.setFloor(Blocks.stone.asFloor());
            tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);
        });
        state.rules.sector=strict.getStartSector(); strict.applyRules(state.rules); logic.play();
        base(strict,strict.startSector,0);
    }

    void tick(int count){for(int i=0;i<count;i++){Time.update();Groups.build.update();Groups.powerGraph.update();}}

    ObjectSet<UnlockableContent> known(UnlockableContent... values){
        ObjectSet<UnlockableContent> set=ObjectSet.with(values);
        for(var value:values) IndustrialResearch.discover(value,strict);
        return set;
    }

    void fund(UnlockableContent value, ObjectSet<UnlockableContent> known){ fund(value,known,new ObjectSet<>()); }
    void fund(UnlockableContent value, ObjectSet<UnlockableContent> known, ObjectSet<UnlockableContent> visiting){
        if(IndustrialResearch.unlocked(value,strict))return;
        assertTrue(visiting.add(value),"Research cycle: "+value.name);
        var node=IndustrialResearch.tree(strict).nodes.get(value); assertNotNull(node,value.name);
        if(node.parent!=null)fund(node.parent.content,known,visiting);
        for(var objective:node.objectives){
            if(objective instanceof Researched r)fund(r.content,known,visiting);
            if(objective instanceof Discovered d)assertTrue(IndustrialResearch.discovered(d.content,strict),"Unobtainable prerequisite for "+value.name+": "+d.content.name);
        }
        for(int iteration=0;iteration<100&&!IndustrialResearch.unlocked(value,strict);iteration++){
            for(var req:node.requirements){assertTrue(known.contains(req.item),"Research consumes unavailable material: "+value.name+"/"+req.item.name);Team.sharded.core().items.set(req.item,Math.min(4000,req.amount));}
            assertTrue(IndustrialResearch.canResearch(node),value.name);
            IndustrialResearch.spend(node);
        }
        assertTrue(IndustrialResearch.unlocked(value,strict),value.name); visiting.remove(value);
    }

    @Test
    void separateWorldsAndProfilesNeverConvertLegacy(){
        assertNotSame(strict.sectors.get(170),Planets.industrialSerpulo.sectors.get(170));
        assertNotEquals(saves.getSectorFile(strict.getStartSector()),saves.getSectorFile(Planets.industrialSerpulo.getStartSector()));
        assertNotEquals(IndustrialResearch.settingsKey(strict),IndustrialResearch.settingsKey(Planets.industrialSerpulo));
        assertEquals(IndustrialResearch.settingsKey,IndustrialResearch.settingsKey(Planets.industrialSerpulo));
        IndustrialResearch.discover(Items.thorium,Planets.industrialSerpulo);
        assertFalse(IndustrialResearch.discovered(Items.thorium,strict));
        assertTrue(IndustrialResearch.available(Blocks.blastDrill,Planets.industrialSerpulo));
        assertFalse(IndustrialResearch.available(Blocks.blastDrill,strict));
        assertTrue(strict.id>Planets.industrialSerpulo.id);
    }

    @Test
    void newStrictWorldStartsLimitedAndBuildValidationEnforcesUnlocks(){
        arena();var core=Team.sharded.core();assertEquals(100,core.items.get(Items.copper));
        assertTrue(Blocks.mechanicalDrill.unlockedNowHost());assertTrue(Blocks.conveyor.unlockedNowHost());
        assertFalse(Blocks.graphitePress.unlockedNowHost());assertFalse(Blocks.siliconSmelter.unlockedNowHost());
        Unit builder=UnitTypes.alpha.create(Team.sharded);
        Build.beginPlace(builder,Blocks.graphitePress,Team.sharded,20,20,0,null);
        assertSame(Blocks.air,world.tile(20,20).block());
        var known=known(Items.copper,Items.lead,Items.coal,Items.sand);
        fund(Blocks.graphitePress,known);
        core.items.set(Items.copper,100);core.items.set(Items.lead,100);
        Build.beginPlace(builder,Blocks.graphitePress,Team.sharded,20,20,0,null);
        assertTrue(world.tile(20,20).build instanceof ConstructBuild);
        ((ConstructBuild)world.tile(20,20).build).construct(builder,core,1f,null);
        assertSame(Blocks.graphitePress,world.tile(20,20).block());
        assertEquals(25,core.items.get(Items.copper));assertEquals(70,core.items.get(Items.lead));
        assertTrue(IndustrialCampaign.canExplore(strict.getStartSector().near().first()));
        assertFalse(state.rules.waves);
    }

    @Test
    void partialPaymentPersistsAndNeverUsesLegacyOrClassicMoney(){
        var own=base(strict,170,100);var free=base(Planets.industrialSerpulo,170,1000);var classic=base(Planets.serpulo,170,1000);
        for(var value:new UnlockableContent[]{Items.copper,Items.lead,Items.coal})IndustrialResearch.discover(value,strict);
        var node=IndustrialResearch.tree(strict).nodes.get(Blocks.graphitePress);
        assertEquals(100,IndustrialResearch.spend(node).get(Items.copper));
        assertFalse(IndustrialResearch.unlocked(Blocks.graphitePress,strict));
        Core.settings.forceSave();Core.settings=settings();
        node=IndustrialResearch.tree(strict).nodes.get(Blocks.graphitePress);
        assertEquals(100,node.finishedRequirements[0].amount);
        own.info.items.set(Items.copper,2400);own.info.items.set(Items.lead,900);
        assertEquals(node.requirements[0].amount + node.requirements[1].amount - 100,IndustrialResearch.spend(node).total);
        assertTrue(IndustrialResearch.unlocked(Blocks.graphitePress,strict));
        assertEquals(1000,free.info.items.get(Items.copper));assertEquals(1000,classic.info.items.get(Items.copper));
        assertFalse(IndustrialResearch.unlocked(Blocks.graphitePress,Planets.industrialSerpulo));
        assertFalse(Core.settings.has("graphite-press-unlocked"));assertFalse(Core.settings.has("req-graphite-press-copper"));
    }

    @Test
    void nativeProductionAndMiningReachGraphiteSiliconTitaniumThorium(){
        arena();var materials=known(Items.copper,Items.lead,Items.coal,Items.sand);
        fund(Blocks.graphitePress,materials);
        world.tile(20,20).setBlock(Blocks.graphitePress,Team.sharded);world.tile(20,20).build.items.add(Items.coal,10);tick(400);
        assertTrue(IndustrialResearch.discovered(Items.graphite,strict));materials.add(Items.graphite);
        fund(Blocks.combustionGenerator,materials);fund(Blocks.siliconSmelter,materials);
        world.tile(30,20).setBlock(Blocks.siliconSmelter,Team.sharded);world.tile(32,20).setBlock(Blocks.combustionGenerator,Team.sharded);
        world.tile(30,20).build.items.add(Items.coal,10);world.tile(30,20).build.items.add(Items.sand,20);world.tile(32,20).build.items.add(Items.coal,10);tick(500);
        assertTrue(IndustrialResearch.discovered(Items.silicon,strict));materials.add(Items.silicon);
        fund(Blocks.pneumaticDrill,materials);
        for(int x=40;x<42;x++)for(int y=20;y<22;y++)world.tile(x,y).setOverlay(Blocks.oreTitanium);
        world.tile(40,20).setBlock(Blocks.pneumaticDrill,Team.sharded);tick(4000);
        assertTrue(IndustrialResearch.discovered(Items.titanium,strict));materials.add(Items.titanium);
        fund(Blocks.laserDrill,materials);
        for(int x=50;x<53;x++)for(int y=20;y<23;y++)world.tile(x,y).setOverlay(Blocks.oreThorium);
        world.tile(51,21).setBlock(Blocks.laserDrill,Team.sharded);
        // Two adjacent real combustion generators supply the laser drill (no free power).
        world.tile(53,21).setBlock(Blocks.combustionGenerator,Team.sharded);world.tile(51,23).setBlock(Blocks.combustionGenerator,Team.sharded);
        world.tile(53,21).build.items.add(Items.coal,10);world.tile(51,23).build.items.add(Items.coal,10);tick(4000);
        assertTrue(IndustrialResearch.discovered(Items.thorium,strict));materials.add(Items.thorium);
        fund(Blocks.blastDrill,materials);
        assertTrue(Blocks.blastDrill.unlockedNowHost());
    }

    @Test
    void treeReparentsCivilNodesAndHasNoCyclesOrMilitaryConditions(){
        var canonical=Blocks.siliconSmelter.techNode;int count=TechTree.all.size,roots=TechTree.roots.size;
        var tree=IndustrialResearch.tree(strict);
        assertSame(canonical,Blocks.siliconSmelter.techNode);assertEquals(count,TechTree.all.size);assertEquals(roots,TechTree.roots.size);
        assertSame(Blocks.combustionGenerator,tree.nodes.get(Blocks.pulverizer).parent.content);
        assertSame(Blocks.kiln,tree.nodes.get(Blocks.waterExtractor).parent.content);
        assertSame(Blocks.mechanicalPump,tree.nodes.get(Blocks.cryofluidMixer).parent.content);
        assertSame(Blocks.airFactory,tree.nodes.get(UnitTypes.mono).parent.content);
        ObjectSet<TechTree.TechNode> seen=new ObjectSet<>();tree.root.each(n->assertTrue(seen.add(n),n.content.name));
        assertEquals(tree.nodes.size,seen.size);
        for(var node:tree.nodes.values()){
            assertTrue(IndustrialContent.industrial(node.content),node.content.name);
            assertFalse(node.objectives.contains(o->o instanceof SectorComplete||o instanceof OnSector||o instanceof Research));
        }
    }

    @Test
    void generatedInitialFrontierContainsEssentialRawMaterials(){
        ObjectSet<Item> found=new ObjectSet<>();Seq<Sector> queue=Seq.with(strict.getStartSector());ObjectSet<Sector> visited=new ObjectSet<>();
        int generated=0;
        while(queue.any()&&generated<20){
            Sector next=queue.remove(0);if(!visited.add(next))continue;
            logic.reset();world.loadSector(next);generated++;
            for(var tile:world.tiles)if(tile.drop()!=null)found.add(tile.drop());
            if(found.contains(Items.copper)&&found.contains(Items.lead)&&found.contains(Items.coal)&&found.contains(Items.sand)&&found.contains(Items.titanium)&&found.contains(Items.thorium))break;
            for(var neighbor:next.near())if(!visited.contains(neighbor))queue.add(neighbor);
        }
        for(var item:new Item[]{Items.copper,Items.lead,Items.coal,Items.sand,Items.titanium,Items.thorium})assertTrue(found.contains(item),item.name);
        System.out.println("Strict frontier resource survey: "+generated+" generated sectors, "+visited);
    }

    @Test
    void nativeSectorSaveReloadKeepsStrictStateAndLegacyStaysUnrestricted(){
        arena();known(Items.copper,Items.lead,Items.coal);Team.sharded.core().items.set(Items.copper,55);
        var node=IndustrialResearch.tree(strict).nodes.get(Blocks.graphitePress);IndustrialResearch.spend(node);
        var file=directory.child("strict.msav");SaveIO.save(file);Core.settings.forceSave();
        logic.reset();Core.settings=settings();SaveIO.load(file,world.makeSectorContext(strict.getStartSector()));
        assertSame(strict,state.getPlanet());assertFalse(Blocks.graphitePress.unlockedNowHost());
        assertEquals(55,IndustrialResearch.tree(strict).nodes.get(Blocks.graphitePress).finishedRequirements[0].amount);
        assertEquals(0,Team.sharded.core().items.get(Items.copper));
        logic.reset();state.rules.sector=Planets.industrialSerpulo.getStartSector();Planets.industrialSerpulo.applyRules(state.rules);
        assertTrue(Blocks.blastDrill.unlockedNowHost());assertTrue(UnitTypes.oct.unlockedNowHost());
        assertTrue(IndustrialResearch.unrestricted());
    }

    @Test
    void factoryConfigurationsRejectUnresearchedUnits(){
        arena();var materials=known(Items.copper,Items.lead,Items.coal,Items.sand,Items.graphite,Items.silicon);
        fund(Blocks.airFactory,materials);
        world.tile(20,30).setBlock(Blocks.airFactory,Team.sharded);
        var factory=(UnitFactory.UnitFactoryBuild)world.tile(20,30).build;
        assertEquals(-1,factory.currentPlan);factory.configured(null,UnitTypes.mono);assertEquals(-1,factory.currentPlan);
        fund(UnitTypes.mono,materials);factory.configured(null,UnitTypes.mono);assertSame(UnitTypes.mono,factory.unit());
        factory.configured(null,UnitTypes.flare);assertSame(UnitTypes.mono,factory.unit());
        assertFalse(Blocks.constructor.unlockedNowHost());
    }
}
