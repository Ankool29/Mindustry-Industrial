import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.game.IndustrialDiagnostics.*;
import mindustry.io.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialDiagnosticsTests{
    Settings previous;
    Fi directory;
    Saves saves;
    ObjectMap<Sector,SectorInfo> infos=new ObjectMap<>();
    ObjectMap<Sector,Saves.SaveSlot> slots=new ObjectMap<>();
    Planet strict;
    Sector source,target;

    @BeforeAll static void init(){ApplicationTests.launchApplication();}
    @BeforeEach void setup(){
        logic.reset();Time.setDeltaProvider(()->1f);previous=Core.settings;
        directory=previous.getDataDirectory().child("m7b-test");directory.deleteDirectory();
        Core.settings=new Settings();Core.settings.setJson(JsonIO.json);Core.settings.setDataDirectory(directory);Core.settings.load();
        if(Core.assets==null)Core.assets=new AssetManager();saves=new Saves();strict=Planets.industrialSerpuloProgressive;
        for(var p:new Planet[]{strict,Planets.industrialSerpulo,Planets.serpulo})for(var s:p.sectors){
            infos.put(s,s.info);slots.put(s,s.save);s.info=new SectorInfo();s.save=null;IndustrialCampaign.prepareInfo(s);
        }
        source=strict.getStartSector();target=source.near().first();
    }
    @AfterEach void cleanup(){logic.reset();infos.each((s,i)->s.info=i);slots.each((s,i)->s.save=i);Core.settings=previous;directory.deleteDirectory();}
    void base(Sector s){
        s.info=new SectorInfo();s.info.waves=s.info.attack=false;s.info.hasCore=true;s.info.storageCapacity=4000;
        s.save=saves.new SaveSlot(directory.child(s.planet.name+s.id+".msav"));
    }
    void rate(ObjectMap<Item,SectorInfo.ExportStat> map,Item item,float value){var stat=new SectorInfo.ExportStat();stat.mean=value;map.put(item,stat);}
    void arena(Sector s){
        logic.reset();world.loadGenerator(48,48,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);});
        state.rules.sector=s;strict.applyRules(state.rules);state.set(State.playing);
    }
    void tick(int amount){for(int i=0;i<amount;i++){Time.update();state.updateId++;state.tick++;Groups.build.update();Groups.powerGraph.update();state.rules.sector.info.update();}}

    @Test void exactNativeValuesAndIndependentOfferWithNoMutation(){
        base(source);base(target);source.info.destination=target;source.info.items.set(Items.coal,17);
        rate(source.info.rawProduction,Items.coal,3.25f);rate(source.info.production,Items.coal,-1.5f);
        rate(source.info.export,Items.coal,2f);rate(target.info.imports,Items.coal,.5f);
        var before=JsonIO.write(source.info);var targetBefore=JsonIO.write(target.info);
        var r=IndustrialDiagnostics.read(strict);var a=r.colony(source);var b=r.colony(target);
        assertEquals(Evidence.nativeEstimate,a.evidence);assertEquals(17,a.material(Items.coal).stock);
        assertEquals(195f,a.material(Items.coal).grossPerMinute);assertEquals(-90f,a.material(Items.coal).netPerMinute);
        assertEquals(90f,a.material(Items.coal).netWithdrawalPerMinute);assertEquals(120f,a.material(Items.coal).exportPerMinute);
        assertEquals(30f,b.material(Items.coal).receivedPerMinute);assertEquals(120f,b.material(Items.coal).offeredPerMinute);
        assertSame(target,a.destination);assertNull(source.info.importRateCache);assertNull(target.info.importRateCache);
        assertEquals(before,JsonIO.write(source.info));assertEquals(targetBefore,JsonIO.write(target.info));
        assertFalse(Core.settings.modified());
    }
    @Test void activeCoreOverridesStoredMetadataAndSnapshotIsDetached(){
        base(source);arena(source);source.info.items.set(Items.copper,999);Team.sharded.core().items.set(Items.copper,77);
        var c=IndustrialDiagnostics.read(strict).colony(source);assertTrue(c.active);assertEquals(Evidence.activeObservation,c.evidence);
        assertEquals(77,c.material(Items.copper).stock);c.material(Items.copper).stock=10000;c.materials.clear();
        assertEquals(77,Team.sharded.core().items.get(Items.copper));assertEquals(999,source.info.items.get(Items.copper));
    }
    @Test void freshnessIsSessionOnlyAndTracksActualNativeSample(){
        base(source);arena(source);assertEquals(0,IndustrialDiagnostics.lastSample(source));
        tick(65);assertTrue(IndustrialDiagnostics.lastSample(source)>0);
        var before=JsonIO.write(source.info);Core.settings.forceSave();byte[] data=Core.settings.getSettingsFile().readBytes();
        IndustrialDiagnostics.measured(source);IndustrialDiagnostics.read(strict);
        assertEquals(before,JsonIO.write(source.info));assertArrayEquals(data,Core.settings.getSettingsFile().readBytes());assertFalse(Core.settings.modified());
        source.info=new SectorInfo();assertEquals(0,IndustrialDiagnostics.lastSample(source));
    }
    @Test void oldSavesHaveUnknownMeasurementAgeAndNoDiskReadNeeded(){
        base(source);source.info.items.set(Items.copper,32);assertFalse(source.save.file.exists());
        var c=IndustrialDiagnostics.read(strict).colony(source);assertEquals(0,c.sampleNanos);assertFalse(c.platformsKnown);assertTrue(c.platforms.isEmpty());
        assertFalse(source.save.file.exists());
    }
    @Test void originalAndFreeCampaignsHaveNoDiagnosticOrInstrumentationState(){
        for(var planet:new Planet[]{Planets.serpulo,Planets.industrialSerpulo}){
            var s=planet.getStartSector();base(s);s.info.items.set(Items.copper,12);var before=JsonIO.write(s.info);
            assertFalse(IndustrialDiagnostics.enabled(planet));assertThrows(IllegalArgumentException.class,()->IndustrialDiagnostics.read(planet));
            IndustrialDiagnostics.measured(s);assertEquals(0,IndustrialDiagnostics.lastSample(s));assertEquals(before,JsonIO.write(s.info));
        }
        assertFalse(Core.settings.modified());
    }
    @Test void sourceEligibilityAndMultipleOffersMatchNativeWithoutRefreshingCaches(){
        base(source);base(target);var extra=strict.sectors.get(15);base(extra);
        source.info.destination=target;extra.info.destination=target;rate(source.info.export,Items.copper,1);rate(extra.info.export,Items.copper,2);
        rate(target.info.imports,Items.copper,.25f);
        assertEquals(180,IndustrialDiagnostics.read(strict).colony(target).material(Items.copper).offeredPerMinute);
        extra.info.hasCore=false;
        assertEquals(60,IndustrialDiagnostics.read(strict).colony(target).material(Items.copper).offeredPerMinute);
        assertEquals(15,IndustrialDiagnostics.read(strict).colony(target).material(Items.copper).receivedPerMinute);
        assertNull(target.info.importRateCache);
    }
    @Test void nativePlatformLimitsConditionsAndLocalAnimationAreReadOnly(){
        base(source);base(target);arena(source);source.info.destination=target;target.info.destination=source;rate(target.info.export,Items.copper,2);
        world.tile(18,18).setBlock(Blocks.advancedLaunchPad,Team.sharded);
        LaunchPadBuild launch=(LaunchPadBuild)world.tile(18,18).build;launch.items.add(Items.copper,30);launch.efficiency=0;
        world.tile(30,30).setBlock(Blocks.landingPad,Team.sharded);
        LandingPadBuild landing=(LandingPadBuild)world.tile(30,30).build;landing.config=Items.copper;landing.items.add(Items.copper,10);landing.cooldown=.5f;
        String before=JsonIO.write(source.info);var c=IndustrialDiagnostics.read(strict).colony(source);
        assertEquals(2,c.platforms.size);var l=c.platforms.find(p->p.launch);var r=c.platforms.find(p->!p.launch);
        assertEquals(200,l.nominalPerMinute);assertEquals(30,l.buffered);assertEquals(9f,l.liquidPerSecond,.0001f);
        assertTrue(l.conditions.contains("loading"));assertTrue(l.conditions.contains("chargeblocked"));
        assertEquals(1200,r.nominalPerMinute);assertEquals(1500,r.requiredLiquid);assertTrue(r.conditions.contains("water"));assertTrue(r.conditions.contains("outputoccupied"));
        assertFalse(r.conditions.contains("nooffer"));assertEquals(30,launch.items.get(Items.copper));assertEquals(10,landing.items.get(Items.copper));
        assertEquals(before,JsonIO.write(source.info));assertNull(source.info.importRateCache);
        landing.arriving=Items.copper;assertSame(Items.copper,IndustrialDiagnostics.read(strict).colony(source).platforms.find(p->!p.launch).arriving);
    }
    @Test void snapshotsDetectNativeCapacityWithoutCappingStocks(){
        base(source);source.info.items.set(Items.graphite,4000);var m=IndustrialDiagnostics.read(strict).colony(source).material(Items.graphite);
        assertTrue(m.coreSaturated);assertEquals(4000,m.stock);assertEquals(4000,source.info.items.get(Items.graphite));
    }
    @Test void m6dWorldSettingsResearchAndExpansionRemainUnchanged(){
        base(source);arena(source);Team.sharded.core().items.set(Items.copper,450);IndustrialResearch.discover(Items.coal,strict);
        var ledger=IndustrialExpansion.ledger(strict);int count=ledger.established;var tree=IndustrialResearch.tree(strict);int nodes=tree.nodes.size;
        Fi file=directory.child("m6d-world.msav");SaveIO.save(file);Core.settings.forceSave();
        byte[] worldBefore=file.readBytes(),settingsBefore=Core.settings.getSettingsFile().readBytes();
        for(int i=0;i<100;i++)IndustrialDiagnostics.read(strict);
        assertArrayEquals(worldBefore,file.readBytes());assertArrayEquals(settingsBefore,Core.settings.getSettingsFile().readBytes());
        assertEquals(count,IndustrialExpansion.ledger(strict).established);assertEquals(nodes,IndustrialResearch.tree(strict).nodes.size);
        assertTrue(IndustrialResearch.discovered(Items.coal,strict));assertEquals(450,Team.sharded.core().items.get(Items.copper));
        SaveIO.load(file,world.makeSectorContext(source));state.set(State.playing);
        assertEquals(450,IndustrialDiagnostics.read(strict).colony(source).material(Items.copper).stock);
    }
    @Test void realFactoryActiveInactiveAndReturnProtocolUsesIsolatedCheckpoint(){
        base(source);base(target);arena(source);
        world.tile(8,6).setBlock(Blocks.conveyor,Team.sharded,2);
        world.tile(9,6).setBlock(Blocks.graphitePress,Team.sharded);world.tile(9,6).build.items.add(Items.coal,10);
        tick(700);
        int produced=Team.sharded.core().items.get(Items.graphite);
        assertTrue(produced>0,"Factory must physically deliver graphite to core");assertEquals(0,world.tile(9,6).build.items.get(Items.coal));
        Fi checkpoint=directory.child("factory.msav");SaveIO.save(checkpoint);String remembered=JsonIO.write(source.info);
        assertTrue(source.info.production.get(Items.graphite).mean>0);
        tick(8000);assertEquals(produced,Team.sharded.core().items.get(Items.graphite));
        assertEquals(0,source.info.production.get(Items.graphite).mean);
        source.info=JsonIO.read(SectorInfo.class,remembered);arena(target);
        for(int turn=0;turn<3;turn++){
            universe.runTurn();String before=JsonIO.write(source.info);var c=IndustrialDiagnostics.read(strict).colony(source);
            assertEquals(Evidence.nativeEstimate,c.evidence);assertEquals(source.info.items.get(Items.graphite),c.material(Items.graphite).stock);
            assertEquals(before,JsonIO.write(source.info));
        }
        assertTrue(source.info.items.get(Items.graphite)>produced,"Native extrapolation persists despite empty machine input");
        int projected=source.info.items.get(Items.graphite);SaveIO.load(checkpoint,world.makeSectorContext(source));state.set(State.playing);
        assertEquals(projected,Team.sharded.core().items.get(Items.graphite));assertEquals(0,world.tile(9,6).build.items.get(Items.coal));
        assertEquals(Evidence.activeObservation,IndustrialDiagnostics.read(strict).colony(source).evidence);
        Log.info("M7B_PROTOCOL factory real=@ native-offscreen=@ returned=@",produced,projected,Team.sharded.core().items.get(Items.graphite));
    }
    @Test void strictReceptionNoLongerMaterializesNativeMeansWithOrWithoutDiagnosticReads(){
        int[] output=new int[2];
        for(int trial=0;trial<2;trial++){
            base(source);base(target);arena(target);source.info.destination=target;rate(source.info.export,Items.copper,2);
            world.tile(20,20).setBlock(Blocks.landingPad,Team.sharded);
            LandingPadBuild pad=(LandingPadBuild)world.tile(20,20).build;pad.config=Items.copper;pad.liquids.add(Liquids.water,1500);
            for(int i=0;i<3500;i++){tick(1);if(trial==1&&i%60==0)IndustrialDiagnostics.read(strict);}
            output[trial]=pad.items.get(Items.copper);assertEquals(0,output[trial]);assertEquals(1500,pad.liquids.get(Liquids.water),.01f);
            var c=IndustrialDiagnostics.read(strict).colony(target);assertTrue(c.material(Items.copper).receivedPerMinute == 0);
            assertEquals(0,source.info.items.get(Items.copper));
        }
        assertEquals(output[0],output[1]);Log.info("M7C_PROTOCOL estimated reception suppressed: @/@",output[0],output[1]);
    }
    @Test void fiveTwentyFiftyHundredSyntheticColoniesReturnOnlyDetachedData(){
        for(int n:new int[]{5,20,50,100}){
            for(Sector s:strict.sectors)s.save=null;
            for(int i=0;i<n;i++){var s=strict.sectors.get(i);base(s);s.info.items.set(Items.copper,100+i);rate(s.info.production,Items.copper,i);}
            var r=IndustrialDiagnostics.read(strict);assertEquals(n,r.colonies.size);
            for(int i=0;i<n;i++){assertEquals(100+i,r.colonies.get(i).material(Items.copper).stock);assertEquals(100+i,strict.sectors.get(i).info.items.get(Items.copper));}
            assertFalse(Core.settings.modified());
        }
    }
}
