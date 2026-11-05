import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.game.*;
import mindustry.game.IndustrialResearch.*;
import mindustry.game.Objectives.*;
import mindustry.io.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialResearchTests{
    private Settings previous;
    private Fi directory;
    private Saves saves;
    private ObjectMap<Sector, SectorInfo> infos;
    private ObjectMap<Sector, Saves.SaveSlot> slots;

    @BeforeAll
    static void init(){ ApplicationTests.launchApplication(); }

    @BeforeEach
    void setup(){
        logic.reset();
        previous = Core.settings;
        directory = previous.getDataDirectory().child("m6b-research-test");
        directory.deleteDirectory();
        Core.settings = freshSettings();
        if(Core.assets == null) Core.assets = new AssetManager();
        saves = new Saves();
        infos = new ObjectMap<>(); slots = new ObjectMap<>();
        for(var planet : new Planet[]{Planets.serpulo, Planets.industrialSerpulo}){
            for(var sector : planet.sectors){
                infos.put(sector, sector.info); slots.put(sector, sector.save);
                sector.info = new SectorInfo(); sector.save = null;
            }
        }
    }

    private Settings freshSettings(){
        Settings settings = new Settings();
        settings.setJson(JsonIO.json);
        settings.setDataDirectory(directory);
        settings.load();
        return settings;
    }

    @AfterEach
    void clean(){
        logic.reset();
        infos.each((sector, info) -> sector.info = info);
        slots.each((sector, slot) -> sector.save = slot);
        Core.settings = previous;
        directory.deleteDirectory();
    }

    private Sector base(Planet planet, int index, int copper){
        Sector sector = planet.sectors.get(index);
        sector.save = saves.new SaveSlot(directory.child(planet.name + "-" + index + ".msav"));
        sector.info.hasCore = true;
        sector.info.storageCapacity = 4000;
        IndustrialCampaign.prepareInfo(sector);
        sector.info.items.add(Items.copper, copper);
        return sector;
    }

    @Test
    void discoveriesNeverWriteClassicUnlocksAndClassicUnlocksNeverDiscoverIndustry(){
        boolean classic = Items.coal.unlocked();
        state.rules.sector = Planets.industrialSerpulo.getStartSector();
        Items.coal.unlock();
        Liquids.oil.quietUnlock();
        assertTrue(IndustrialResearch.discovered(Items.coal));
        assertTrue(IndustrialResearch.discovered(Liquids.oil));
        assertEquals(classic, Items.coal.unlocked());
        assertFalse(Core.settings.has(Items.coal.name + "-unlocked"));
        logic.reset();
        assertFalse(IndustrialResearch.discovered(Items.copper));
        boolean copperWasUnlocked = Items.copper.unlocked();
        Items.copper.unlock();
        assertFalse(IndustrialResearch.discovered(Items.copper));
        if(!copperWasUnlocked) Items.copper.clearUnlock();
    }

    @Test
    void detachedNativeNodesKeepAllClassicPointersPaymentsAndAutoUnlockLists(){
        var classic = Blocks.mechanicalDrill.techNode;
        int all = TechTree.all.size, roots = TechTree.roots.size, nodes = Blocks.mechanicalDrill.techNodes.size;
        Core.settings.put("req-mechanical-drill-copper", 9);
        var tree = IndustrialResearch.tree();
        var node = tree.nodes.get(Blocks.mechanicalDrill);
        assertSame(Blocks.mechanicalDrill, node.content);
        assertSame(classic, Blocks.mechanicalDrill.techNode);
        assertEquals(nodes, Blocks.mechanicalDrill.techNodes.size);
        assertEquals(all, TechTree.all.size);
        assertEquals(roots, TechTree.roots.size);
        assertEquals(0, node.finishedRequirements[0].amount);
        assertEquals(9, Core.settings.getInt("req-mechanical-drill-copper"));
        assertNotSame(classic.requirements, node.requirements);
        assertEquals(10, classic.requirements[0].amount);
        assertSame(Blocks.airFactory, tree.nodes.get(UnitTypes.mono).parent.content);
        assertFalse(tree.nodes.containsKey(Blocks.duo));
        for(var n : tree.nodes.values()){
            assertFalse(TechTree.all.contains(n));
            assertFalse(n.objectives.contains(o -> o instanceof Research || o instanceof SectorComplete || o instanceof OnSector));
        }
    }

    @Test
    void partialPaymentsUseOnlyIndustrialStocksAndSurviveSettingsReload(){
        Sector industry = base(Planets.industrialSerpulo, 170, 4);
        Sector classic = base(Planets.serpulo, 170, 900);
        IndustrialResearch.discover(Items.copper);
        var node = IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill);
        assertEquals(4, IndustrialResearch.availableItems().get(Items.copper));
        assertEquals(4, IndustrialResearch.spend(node).get(Items.copper));
        assertEquals(4, node.finishedRequirements[0].amount);
        assertFalse(IndustrialResearch.unlocked(Blocks.mechanicalDrill));
        assertEquals(900, classic.info.items.get(Items.copper));
        assertEquals(0, industry.info.items.get(Items.copper));
        Core.settings.forceSave();
        Core.settings = freshSettings();
        Core.settings.load();
        assertTrue(IndustrialResearch.discovered(Items.copper));
        node = IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill);
        assertEquals(4, node.finishedRequirements[0].amount);
        industry.info.items.add(Items.copper, 6);
        assertEquals(6, IndustrialResearch.spend(node).get(Items.copper));
        assertTrue(IndustrialResearch.unlocked(Blocks.mechanicalDrill));
        assertEquals(900, classic.info.items.get(Items.copper));
        assertEquals(0, IndustrialResearch.spend(node).total);
        assertFalse(Core.settings.has("mechanical-drill-unlocked"));
        assertFalse(Core.settings.has("req-mechanical-drill-copper"));
        Core.settings.forceSave();
        Core.settings = freshSettings(); Core.settings.load();
        assertTrue(IndustrialResearch.unlocked(Blocks.mechanicalDrill));
        assertEquals(10, IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill).finishedRequirements[0].amount);
    }

    @Test
    void staleTreeCannotSpendTwiceOrOverwriteFreshPayments(){
        Sector sector = base(Planets.industrialSerpulo, 170, 3);
        IndustrialResearch.discover(Items.copper);
        var a = IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill);
        var b = IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill);
        assertEquals(3, IndustrialResearch.spend(a).total);
        sector.info.items.add(Items.copper, 3);
        assertEquals(3, IndustrialResearch.spend(b).total);
        assertEquals(6, b.finishedRequirements[0].amount);
    }

    @Test
    void objectivesAreScopedAndExpansionDoesNotCountClassicBases(){
        assertFalse(new EstablishedBases(1).complete());
        base(Planets.serpulo, 170, 1000);
        assertFalse(new EstablishedBases(1).complete());
        base(Planets.industrialSerpulo, 170, 100);
        assertTrue(new EstablishedBases(1).complete());
        assertFalse(new EstablishedBases(2).complete());
        base(Planets.industrialSerpulo, 171, 100);
        assertTrue(new EstablishedBases(2).complete());
        assertFalse(new Discovered(Items.thorium).complete());
        IndustrialResearch.discover(Items.thorium);
        assertTrue(new Discovered(Items.thorium).complete());
        assertFalse(new Researched(Blocks.mechanicalDrill).complete());
    }

    @Test
    void currentAndOfflineIndustrialCoresUseNativeWithdrawalsWithoutClassicSpending(){
        Sector start = Planets.industrialSerpulo.getStartSector();
        world.loadSector(start); logic.play();
        start.save = saves.new SaveSlot(directory.child("active.msav")); start.info.hasCore = true;
        Sector offline = base(Planets.industrialSerpulo, 171, 4);
        base(Planets.serpulo, 170, 1000);
        assertEquals(100, Team.sharded.core().items.get(Items.copper));
        IndustrialResearch.discover(Items.copper);
        var node = IndustrialResearch.tree().nodes.get(Blocks.mechanicalDrill);
        assertEquals(10, IndustrialResearch.spend(node).total);
        assertEquals(0, offline.info.items.get(Items.copper));
        assertEquals(94, Team.sharded.core().items.get(Items.copper));
        assertEquals(1000, Planets.serpulo.sectors.get(170).info.items.get(Items.copper));
        assertTrue(offline.info.items.get(Items.copper) >= 0);
        assertTrue(Blocks.blastDrill.unlockedNowHost());
        assertTrue(IndustrialResearch.unrestricted());
        assertTrue(IndustrialCampaign.canExplore(start.near().first()));
    }
}
