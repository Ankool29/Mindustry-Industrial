import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.planet.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialCampaignTests{
    private Saves saves;

    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void reset(){
        logic.reset();
        Core.settings.setJson(JsonIO.json);
        Time.setDeltaProvider(() -> 1f);
        if(Core.assets == null) Core.assets = new AssetManager();
        saves = new Saves();
        for(var sector : Planets.industrialSerpulo.sectors){
            sector.save = null;
            sector.info = new SectorInfo();
            IndustrialCampaign.prepareInfo(sector);
        }
        universe.clearLoadoutInfo();
    }

    @AfterEach
    void clean(){
        logic.reset();
        for(var sector : Planets.industrialSerpulo.sectors){
            saves.getSectorFile(sector).delete();
            sector.save = null;
            sector.clearInfo();
            IndustrialCampaign.prepareInfo(sector);
        }
    }

    private void generate(Sector sector, Sector origin){
        logic.reset();
        world.loadSector(sector);
        sector.info.origin = origin;
        logic.play();
    }

    private Fi save(Sector sector){
        Fi file = saves.getSectorFile(sector);
        SaveIO.save(file);
        sector.save = saves.new SaveSlot(file);
        return file;
    }

    @Test
    void separatePlanetReusesOriginalGeneratorAndSaveNamespaces(){
        var planet = Planets.industrialSerpulo;
        assertTrue(planet instanceof IndustrialPlanet);
        assertEquals(SerpuloPlanetGenerator.class, planet.generator.getClass());
        assertEquals(Planets.serpulo.grid.tiles.length, planet.grid.tiles.length);
        assertEquals(Planets.serpulo.sectorSeed, planet.sectorSeed);
        assertTrue(planet.alwaysUnlocked);
        assertFalse(planet.allowCampaignRules);
        assertNull(planet.techTree, "Do not share the military research tree");
        assertFalse(planet.sectors.contains(Sector::hasEnemyBase));
        assertNotEquals(saves.getSectorFile(planet.getStartSector()), saves.getSectorFile(Planets.serpulo.sectors.get(planet.startSector)));
    }

    @Test
    void campaignStartsWithFiniteStockAndNoMilitaryState(){
        Sector start = Planets.industrialSerpulo.getStartSector();
        generate(start, start);
        assertTrue(state.isCampaign());
        assertTrue(state.rules.industrial);
        assertNotNull(Team.sharded.core());
        assertEquals(100, Team.sharded.core().items.get(Items.copper));
        assertFalse(state.rules.isInfiniteResources(Team.sharded));
        assertEquals(1f, state.rules.buildCostMultiplier);
        assertFalse(state.rules.waves);
        assertFalse(state.rules.attackMode);
        assertFalse(state.rules.canGameOver);
        assertEquals(0, spawner.countSpawns());
        assertFalse(GroupsHaveEnemy());
        save(start);
        assertTrue(start.hasBase());
        assertFalse(start.isAttacked());
        assertFalse(start.info.waves);
        assertFalse(start.info.attack);
        assertTrue(IndustrialCampaign.canExplore(start.near().first()));
    }

    private boolean GroupsHaveEnemy(){
        return mindustry.gen.Groups.unit.contains(u -> u.team != state.rules.defaultTeam) ||
            mindustry.gen.Groups.build.contains(b -> b.team != state.rules.defaultTeam && b.team != Team.derelict);
    }

    @Test
    void industrialAvailabilityDoesNotUnlockClassicResearch(){
        boolean unlocked = Blocks.siliconSmelter.unlocked();
        boolean stored = Core.settings.getBool(Blocks.siliconSmelter.name + "-unlocked");
        boolean itemUnlocked = Items.silicon.unlocked();
        state.rules.sector = Planets.industrialSerpulo.getStartSector();
        Planets.industrialSerpulo.applyRules(state.rules);
        assertTrue(Blocks.siliconSmelter.unlockedNowHost());
        assertTrue(Blocks.siliconSmelter.environmentBuildable());
        assertTrue(Items.silicon.isOnPlanet(Planets.industrialSerpulo));
        Blocks.siliconSmelter.unlock();
        Blocks.siliconSmelter.quietUnlock();
        Items.silicon.unlock();
        assertEquals(unlocked, Blocks.siliconSmelter.unlocked());
        assertEquals(stored, Core.settings.getBool(Blocks.siliconSmelter.name + "-unlocked"));
        assertEquals(itemUnlocked, Items.silicon.unlocked());
        logic.reset();
        state.rules.sector = Planets.serpulo.getStartSector();
        assertEquals(unlocked, Blocks.siliconSmelter.unlockedNowHost());
        assertTrue(Planets.serpulo.allowWaves);
    }

    @Test
    void sectorTravelSavesBothFactoriesAndPreservesStock(){
        Sector a = Planets.industrialSerpulo.getStartSector(), b = a.near().first();
        assertFalse(IndustrialCampaign.canExplore(b));
        generate(a, a);
        var core = Team.sharded.core();
        var tile = world.tile(core.tileX() + 5, core.tileY());
        tile.setBlock(Blocks.graphitePress, Team.sharded);
        tile.build.items.add(Items.coal, 7);
        int x = tile.x, y = tile.y;
        core.items.set(Items.copper, 1500);
        core.items.set(Items.lead, 1000);
        Fi fileA = save(a);
        assertTrue(IndustrialCampaign.canExplore(b));
        assertSame(a, IndustrialCampaign.findLauncher(b, a));
        var loadout = Loadouts.basicShard;
        var price = loadout.requirements();
        assertTrue(price.total > 0);
        int copperBefore = core.items.get(Items.copper);
        a.removeItems(price);
        assertEquals(copperBefore - price.get(Items.copper), core.items.get(Items.copper));
        save(a);

        universe.updateLoadout((CoreBlock)Blocks.coreShard, loadout);
        universe.updateLaunchResources(new ItemSeq());
        generate(b, a);
        assertEquals(0, Team.sharded.core().items.get(Items.copper), "No second free starter stock");
        var tileB = world.tile(Team.sharded.core().tileX() + 5, Team.sharded.core().tileY());
        tileB.setBlock(Blocks.container, Team.sharded);
        tileB.build.items.add(Items.lead, 17);
        int bx = tileB.x, by = tileB.y;
        Fi fileB = save(b);
        logic.reset();
        SaveIO.load(fileA, world.makeSectorContext(a));
        assertSame(a, state.rules.sector);
        assertTrue(state.rules.industrial);
        assertFalse(state.rules.canGameOver);
        assertEquals(7, world.tile(x, y).build.items.get(Items.coal));
        assertEquals(copperBefore - price.get(Items.copper), Team.sharded.core().items.get(Items.copper));
        logic.reset();
        SaveIO.load(fileB, world.makeSectorContext(b));
        assertSame(b, state.rules.sector);
        assertEquals(17, world.tile(bx, by).build.items.get(Items.lead));
        assertFalse(state.rules.waves);
        assertFalse(GroupsHaveEnemy());
    }

    @Test
    void campaignSettingsCannotReactivateInvasions(){
        state.rules.sector = Planets.industrialSerpulo.getStartSector();
        Planets.industrialSerpulo.campaignRules.sectorInvasion = true;
        Planets.industrialSerpulo.campaignRules.rtsAI = true;
        Planets.industrialSerpulo.applyRules(state.rules);
        assertFalse(Planets.industrialSerpulo.campaignRules.sectorInvasion);
        assertFalse(Planets.industrialSerpulo.campaignRules.rtsAI);
        assertFalse(state.rules.waves);
        assertTrue(Planets.serpulo.allowSectorInvasion);
    }
}
