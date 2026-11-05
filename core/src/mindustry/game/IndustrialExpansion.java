package mindustry.game;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.io.*;
import mindustry.type.*;

import java.util.UUID;

import static mindustry.Vars.*;

/** Strict-campaign launch journal. SectorInfo stocks and ledger commit together in native settings. */
public final class IndustrialExpansion{
    public static final String marker = "industrial-expansion-transaction";
    private IndustrialExpansion(){}

    public static class Policy{
        public int freeLandings = 2, copperStep = 250, leadStep = 200, copperCap = 2000, leadCap = 1600;
        public ItemSeq surcharge(int established){
            long step = Math.max(0L, (long)established + 1 - Math.max(2, freeLandings));
            ItemSeq result = new ItemSeq();
            result.set(Items.copper, (int)Math.min(Math.max(0, copperCap), step * Math.max(0, copperStep)));
            result.set(Items.lead, (int)Math.min(Math.max(0, leadCap), step * Math.max(0, leadStep)));
            return result;
        }
    }

    public static class Journal{
        public String id;
        public int source, destination;
        public ItemSeq total = new ItemSeq();
    }

    public static class Ledger{
        public int version = 1, established, grandfathered;
        public Policy policy = new Policy();
        public @Nullable Journal pending;
    }

    public static String key(Planet planet){return "industrial-expansion-v1-" + planet.name;}

    public static Ledger ledger(Planet planet){
        if(!IndustrialCampaign.strict(planet)) throw new IllegalArgumentException("Strict industrial planet required");
        if(Core.settings.has(key(planet))) return Core.settings.getJson(key(planet), Ledger.class, Ledger::new);
        Ledger result = new Ledger();
        // Legacy saves are grandfathered as one economic starting point; no retrospective bill.
        result.grandfathered = planet.sectors.count(s -> s.hasSave() || s.info.attempts > 0);
        result.established = result.grandfathered == 0 ? 0 : 1;
        persist(planet, result);
        return result;
    }

    private static void persist(Planet planet, Ledger ledger){
        Core.settings.putJson(key(planet), ledger);
        // Unlike forceSave, propagate I/O errors even when a global error handler is installed.
        Core.settings.saveValues();
    }

    public static ItemSeq surcharge(Sector destination){
        return !IndustrialCampaign.strict(destination.planet) || destination.hasBase() ? new ItemSeq() : ledger(destination.planet).policy.surcharge(ledger(destination.planet).established);
    }

    public static ItemSeq total(Sector destination, ItemSeq schematic, ItemSeq cargo){
        ItemSeq result = schematic.copy(); cargo.each(result::add); surcharge(destination).each(result::add); return result;
    }

    /** Called only after confirmation, before switching worlds. No resources are removed here. */
    public static void prepare(Sector source, Sector destination, ItemSeq schematic, ItemSeq cargo){
        if(source == destination || source.planet != destination.planet || !IndustrialCampaign.strict(destination.planet) ||
            !source.hasBase() || destination.hasBase() || !source.isNear(destination) || !IndustrialCampaign.canExplore(destination)){
            throw new IllegalStateException("Invalid industrial departure");
        }
        Ledger ledger = ledger(destination.planet);
        if(ledger.pending != null) throw new IllegalStateException("Industrial departure already pending");
        ItemSeq bill = total(destination, schematic, cargo);
        if(!source.items().has(bill)) throw new IllegalStateException(Core.bundle.get("sector.missingresources"));
        // Make native offline stocks authoritative before writing PREPARED.
        if(source.isBeingPlayed()) control.saves.getCurrent().save();
        Journal journal = new Journal(); journal.id = UUID.randomUUID().toString();
        journal.source = source.id; journal.destination = destination.id; journal.total = bill;
        ledger.pending = journal;
        try{persist(destination.planet, ledger);}catch(Throwable failure){
            ledger.pending = null; Core.settings.putJson(key(destination.planet), ledger); throw failure;
        }
    }

    /** Stamp before the first destination save, never before an existing-sector visit. */
    public static void stamp(Sector destination){
        if(!IndustrialCampaign.strict(destination.planet)) return;
        Ledger ledger = ledger(destination.planet);
        if(ledger.pending != null && ledger.pending.destination == destination.id){
            state.rules.tags.put(marker, ledger.pending.id);
        }
    }

    private static boolean landed(Planet planet, Journal journal){
        Sector target = planet.sectors.get(journal.destination);
        if(target.save == null || !target.save.file.exists()) return false;
        // Read the completed .msav, not in-memory rules from a failed save attempt.
        return journal.id.equals(SaveIO.getMeta(target.save.file).rules.tags.get(marker));
    }

    /** Roll forward a saved landing or cancel an uncompleted one. Idempotent across restarts. */
    public static void recover(Planet planet){
        if(!IndustrialCampaign.strict(planet)) return;
        Ledger ledger = ledger(planet);
        Journal journal = ledger.pending;
        if(journal == null) return;
        if(!landed(planet, journal)){
            ledger.pending = null; persist(planet, ledger); return;
        }
        Sector source = planet.sectors.get(journal.source);
        if(source.isBeingPlayed()) throw new IllegalStateException("Recover expansion before loading its source");
        ItemSeq previous = source.info.items.copy();
        if(!previous.has(journal.total)) throw new IllegalStateException("Industrial launch stock mismatch; journal retained");
        int oldCount = ledger.established;
        try{
            journal.total.each(source.info.items::remove);
            source.saveInfo();
            if(ledger.established < Integer.MAX_VALUE) ledger.established ++;
            ledger.pending = null;
            persist(planet, ledger);
        }catch(Throwable failure){
            source.info.items = previous; source.saveInfo();
            ledger.established = oldCount; ledger.pending = journal;
            Core.settings.putJson(key(planet), ledger);
            throw failure;
        }
    }

    /** After successful initial .msav creation; later visits never call this. */
    public static void established(Sector destination){
        if(!IndustrialCampaign.strict(destination.planet)) return;
        Ledger ledger = ledger(destination.planet);
        if(ledger.pending != null){recover(destination.planet);}else if(ledger.established == 0){
            ledger.established = 1; persist(destination.planet, ledger);
        }
    }
}
