import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.IndustrialExpansion.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.storage.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

class IndustrialExpansionTests{
    Settings previous;
    Fi directory;
    Planet strict;
    Saves saves;
    ObjectMap<Sector, SectorInfo> infos = new ObjectMap<>();
    ObjectMap<Sector, Saves.SaveSlot> slots = new ObjectMap<>();
    Sector source, destination;

    @BeforeAll static void init(){ApplicationTests.launchApplication();}
    Settings settings(){
        Settings result = new Settings();result.setJson(JsonIO.json);result.setDataDirectory(directory);result.load();return result;
    }
    @BeforeEach void setup(){
        logic.reset(); previous=Core.settings;directory=previous.getDataDirectory().child("m6d-test");directory.deleteDirectory();
        Core.settings=settings();strict=Planets.industrialSerpuloProgressive;
        if(Core.assets==null)Core.assets=new AssetManager();saves=new Saves();
        for(var planet:new Planet[]{strict,Planets.industrialSerpulo,Planets.serpulo})for(var s:planet.sectors){
            infos.put(s,s.info);slots.put(s,s.save);s.info=new SectorInfo();s.save=null;IndustrialCampaign.prepareInfo(s);
        }
        source=strict.getStartSector();destination=source.near().first();
    }
    @AfterEach void clean(){
        logic.reset();infos.each((s,i)->s.info=i);slots.each((s,i)->s.save=i);Core.settings=previous;directory.deleteDirectory();
    }
    void base(Sector s){
        s.save=saves.new SaveSlot(directory.child("sector-"+s.id+".msav"));s.info.hasCore=true;s.info.attempts=1;
        s.info.storageCapacity=4000;s.info.items.set(Items.copper,4000);s.info.items.set(Items.lead,4000);s.saveInfo();
    }
    ItemSeq core(){return Loadouts.basicShard.requirements();}
    void count(int value){var l=IndustrialExpansion.ledger(strict);l.established=value;Core.settings.putJson(IndustrialExpansion.key(strict),l);Core.settings.saveValues();}
    void prepare(){IndustrialExpansion.prepare(source,destination,core(),new ItemSeq());}
    void land(){
        world.loadGenerator(40,40,tiles->{tiles.fill();for(var tile:tiles)tile.setFloor(Blocks.stone.asFloor());tiles.get(6,6).setBlock(Blocks.coreShard,Team.sharded);});
        state.rules.sector=destination;strict.applyRules(state.rules);logic.play();destination.info.origin=source;
        IndustrialExpansion.stamp(destination);
        if(destination.save==null)destination.save=saves.new SaveSlot(directory.child("landing-"+destination.id+".msav"));
        SaveIO.save(destination.save.file);
    }
    void restart(){
        logic.reset();Core.settings=settings();for(var s:strict.sectors)s.loadInfo();
    }

    @Test void proposedPricesAndCapsAreExactAndConfigurable(){
        Policy p=new Policy();
        for(int[] v:new int[][]{{0,0,0},{1,0,0},{2,250,200},{3,500,400},{4,750,600},{9,2000,1600},{100,2000,1600},{Integer.MAX_VALUE,2000,1600}}){
            assertEquals(v[1],p.surcharge(v[0]).get(Items.copper));assertEquals(v[2],p.surcharge(v[0]).get(Items.lead));
        }
        p.copperStep=100;p.leadStep=80;p.copperCap=300;p.leadCap=240;
        assertEquals(100,p.surcharge(2).get(Items.copper));assertEquals(240,p.surcharge(99).get(Items.lead));
    }
    @Test void cappedBasicCoreFitsItsNativeCapacityWithNoStorageChanges(){
        int capacity=((CoreBlock)Blocks.coreShard).itemCapacity;assertEquals(4000,capacity);
        var total=core();new Policy().surcharge(100).each(total::add);
        for(var stack:total)assertTrue(stack.amount<=capacity,stack.item.name);
        assertEquals(3000,total.get(Items.copper));assertEquals(2400,total.get(Items.lead));
    }
    @Test void m6cInitialChoicesAndNeighborRulesRemainUnchanged(){
        base(source);assertEquals(6,source.near().size);
        var choices=strict.sectors.select(s->s!=source&&IndustrialCampaign.canExplore(s));
        assertEquals(source.near().size,choices.size);for(var s:choices)assertTrue(source.isNear(s));
        Sector remote=strict.sectors.find(s->s!=source&&!source.isNear(s));assertNotNull(remote);
        assertFalse(IndustrialCampaign.canExplore(remote));
        assertThrows(IllegalStateException.class,()->IndustrialExpansion.prepare(source,remote,core(),new ItemSeq()));
    }
    @Test void m6cMigrationGrandfathersAllExistingBasesWithoutRetrospectiveFees(){
        base(source);for(var s:source.near())base(s);
        int stock=source.info.items.get(Items.copper);var ledger=IndustrialExpansion.ledger(strict);
        assertEquals(7,ledger.grandfathered);assertEquals(1,ledger.established);
        assertEquals(stock,source.info.items.get(Items.copper));assertEquals(7,strict.sectors.count(Sector::hasBase));
        restart();assertEquals(1,IndustrialExpansion.ledger(strict).established);
    }
    @Test void cancellationAndUncompletedDepartureNeverDebitOrCount(){
        base(source);count(2);prepare();assertEquals(4000,source.items().get(Items.copper));
        assertNotNull(IndustrialExpansion.ledger(strict).pending);
        IndustrialExpansion.recover(strict);assertNull(IndustrialExpansion.ledger(strict).pending);
        assertEquals(4000,source.items().get(Items.copper));assertEquals(2,IndustrialExpansion.ledger(strict).established);
    }
    @Test void insufficientResourcesPreventJournalAndDebit(){
        base(source);count(2);source.info.items.set(Items.copper,1249);
        assertThrows(IllegalStateException.class,this::prepare);assertNull(IndustrialExpansion.ledger(strict).pending);
        assertEquals(1249,source.items().get(Items.copper));
    }
    @Test void savedLandingDebitsExactlyOnceAndCounterPersistsAcrossRestart(){
        base(source);count(2);prepare();land();IndustrialExpansion.established(destination);
        assertEquals(2750,source.items().get(Items.copper));assertEquals(3000,source.items().get(Items.lead));
        assertEquals(3,IndustrialExpansion.ledger(strict).established);
        IndustrialExpansion.established(destination);IndustrialExpansion.recover(strict);
        assertEquals(2750,source.items().get(Items.copper));
        restart();IndustrialExpansion.recover(strict);
        assertEquals(2750,source.items().get(Items.copper));assertEquals(3,IndustrialExpansion.ledger(strict).established);
    }
    @Test void interruptedAfterDestinationSaveRollsForwardOnceAfterRestart(){
        base(source);count(3);prepare();land();
        assertEquals(4000,source.info.items.get(Items.copper));restart();IndustrialExpansion.recover(strict);
        assertEquals(2500,source.items().get(Items.copper));assertEquals(2800,source.items().get(Items.lead));
        assertEquals(4,IndustrialExpansion.ledger(strict).established);
        restart();IndustrialExpansion.recover(strict);assertEquals(2500,source.items().get(Items.copper));
    }
    @Test void interruptionBeforeDestinationSaveCancelsAfterRestart(){
        base(source);count(3);prepare();restart();IndustrialExpansion.recover(strict);
        assertEquals(4000,source.items().get(Items.copper));assertEquals(3,IndustrialExpansion.ledger(strict).established);
    }
    @Test void repeatedConfirmCannotCreateSecondPendingDeparture(){
        base(source);count(2);prepare();assertThrows(IllegalStateException.class,this::prepare);
        assertEquals(4000,source.items().get(Items.copper));
    }
    @Test void existingBaseVisitsAreFreeAndAbandonRecolonizeNeverReducesCounter(){
        base(source);base(destination);count(4);
        assertEquals(0,IndustrialExpansion.surcharge(destination).total);
        assertThrows(IllegalStateException.class,this::prepare);assertEquals(4,IndustrialExpansion.ledger(strict).established);
        destination.info.hasCore=false;destination.saveInfo();
        assertEquals(750,IndustrialExpansion.surcharge(destination).get(Items.copper));
        prepare();land();IndustrialExpansion.established(destination);
        assertEquals(5,IndustrialExpansion.ledger(strict).established);
        assertEquals(2250,source.items().get(Items.copper));
    }
    @Test void freeAndClassicCampaignsHaveNoLedgerNoSurchargeNoStockChange(){
        for(var planet:new Planet[]{Planets.industrialSerpulo,Planets.serpulo}){
            Sector s=planet.getStartSector();base(s);
            assertEquals(0,IndustrialExpansion.surcharge(s.near().first()).total);
            assertThrows(IllegalArgumentException.class,()->IndustrialExpansion.ledger(planet));
            IndustrialExpansion.recover(planet);IndustrialExpansion.established(s);
            assertFalse(Core.settings.has(IndustrialExpansion.key(planet)));assertEquals(4000,s.items().get(Items.copper));
        }
    }
    @Test void settingsWriteFailureRetainsJournalAndRetryDoesNotDoubleDebit(){
        base(source);count(2);prepare();land();
        boolean[] failing={false};
        Settings injected=new Settings(){@Override public synchronized void saveValues(){if(failing[0])throw new RuntimeException("Injected I/O failure");super.saveValues();}};
        injected.setJson(JsonIO.json);injected.setDataDirectory(directory);injected.load();Core.settings=injected;failing[0]=true;
        assertThrows(RuntimeException.class,()->IndustrialExpansion.recover(strict));
        assertEquals(4000,source.items().get(Items.copper));assertNotNull(IndustrialExpansion.ledger(strict).pending);
        assertEquals(2,IndustrialExpansion.ledger(strict).established);
        failing[0]=false;IndustrialExpansion.recover(strict);IndustrialExpansion.recover(strict);
        assertEquals(2750,source.items().get(Items.copper));assertEquals(3,IndustrialExpansion.ledger(strict).established);
    }
    @Test void oldDestinationMarkerCannotTurnFailedRecolonizationIntoPayment(){
        base(source);count(2);prepare();land();IndustrialExpansion.established(destination);
        destination.info.hasCore=false;destination.saveInfo();logic.reset();prepare();
        IndustrialExpansion.recover(strict);assertEquals(3,IndustrialExpansion.ledger(strict).established);
        assertEquals(2750,source.items().get(Items.copper));assertNull(IndustrialExpansion.ledger(strict).pending);
    }
}
