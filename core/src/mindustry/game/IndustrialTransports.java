package mindustry.game;

import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.world.blocks.campaign.LaunchPad.*;
import mindustry.world.blocks.campaign.LandingPad.*;
import mindustry.world.blocks.campaign.*;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import static mindustry.Vars.*;

/** Finite manifests, independently durable from native worlds. All mutations run on the simulation thread.
 * A world watermark proves a debit/credit survived a canonical checkpoint. No export mean funds a shipment.
 */
public final class IndustrialTransports{
    private static final String generationTag = "industrial-transport-generation-v1";
    private static final String outTag = "industrial-transport-out-v1", inTag = "industrial-transport-in-v1";
    private static final int magic = 0x49544331, maxPending = 512;
    private static Settings session;
    private static Book book;
    private static Fi journal, identity;
    private static String failure;
    private IndustrialTransports(){}

    public enum Phase{prepared, transit, arriving, credit, overlay}
    public static class Cargo{
        public String item;
        public int amount;
        public Cargo(){}
        Cargo(Item item, int amount){this.item = item.name;this.amount = amount;}
    }
    public static class Shipment{
        public long sequence, incoming;
        public String id;
        public int source, destination, position = -1;
        public Phase phase = Phase.prepared;
        public Seq<Cargo> cargo = new Seq<>();
        public float waterBefore;
        // Optional v1 extensions: old M7C overlays consumed a full receiver charge.
        public float waterCost = -1f;
        public boolean resumedArrival;
        public int total(){int n = 0;for(var c : cargo)n += c.amount;return n;}
    }
    public static class Receiver{
        public int position, amount, capacity;
        public String config, block;
        // Only a canonical world checkpoint can attest an already started native arrival.
        public String arrivalId, arrivalItem;
        public float water, required, paidWater;
        public boolean enabled, busy;
    }
    public static class Checkpoint{
        public int sector;
        public String generation;
        public long outgoing, incoming;
        public Seq<Receiver> receivers = new Seq<>();
    }
    public static class Totals{
        public int sector;
        public long sent, received;
    }
    public static class Book{
        public int version = 1;
        public String epoch = UUID.randomUUID().toString();
        public long next = 1;
        public Seq<Shipment> pending = new Seq<>();
        public Seq<Checkpoint> checkpoints = new Seq<>();
        public Seq<Totals> totals = new Seq<>();
    }
    public static class TransportException extends RuntimeException{
        TransportException(String message){super(message);}
    }
    public static boolean enabled(Planet planet){return planet != null && planet == Planets.industrialSerpuloProgressive;}
    private static boolean active(){return state.isCampaign() && enabled(state.getPlanet()) && !net.client();}
    private static Fi file(Sector sector){return sector.save == null ? null : sector.save.file;}
    private static boolean canonical(Fi file){return active() && file.equals(file(state.getSector()));}
    private static long mark(Rules rules, String tag){return Long.parseLong(rules.tags.get(tag, "0"));}
    private static void mark(String tag, long number){state.rules.tags.put(tag, Long.toString(number));}
    private static Sector sector(int id){return Planets.industrialSerpuloProgressive.sectors.get(id);}
    private static Checkpoint checkpoint(int sector){
        var c = book.checkpoints.find(w -> w.sector == sector);
        if(c == null){c = new Checkpoint();c.sector = sector;book.checkpoints.add(c);}
        return c;
    }
    private static Totals totals(int sector){
        var t = book.totals.find(w -> w.sector == sector);
        if(t == null){t = new Totals();t.sector = sector;book.totals.add(t);}
        return t;
    }
    private static void fail(Throwable cause){
        failure = "Transports suspendus : " + cause.getMessage() + ". Conserver l’export complet des données ; ne pas effacer le journal.";
        Log.err(failure, cause);
        if(active()) state.set(mindustry.core.GameState.State.paused);
        if(!headless && ui != null) Core.app.post(() -> ui.showErrorMessage(failure));
    }
    private static void ensure(){
        if(session == Core.settings){if(failure != null) throw new TransportException(failure);return;}
        session = Core.settings;book = new Book();failure = null;
        // The whole saves directory is already part of the native full-data ZIP export.
        journal = Core.settings.getDataDirectory().child("saves/industrial-transports-v1.bin");
        identity = journal.sibling("industrial-transports-v1.identity");
        try{
            Fi temporary = journal.sibling(journal.name() + ".new");
            if(identity.exists() && !journal.exists()) throw new IOException("journal manquant pour une logistique déjà initialisée");
            if(journal.exists() && !identity.exists()) throw new IOException("identité du journal manquante");
            if(temporary.exists()) throw new IOException("écriture de journal interrompue ; réconciliation manuelle nécessaire");
            if(journal.exists()){
                byte[] bytes = journal.readBytes();
                try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))){
                    if(in.readInt() != magic) throw new IOException("format du journal inconnu");
                    int size = in.readInt();long checksum = in.readLong();
                    if(size < 0 || size != bytes.length - 16) throw new IOException("journal tronqué");
                    byte[] json = new byte[size];in.readFully(json);CRC32 crc = new CRC32();crc.update(json);
                    if(crc.getValue() != checksum) throw new IOException("somme de contrôle du journal incorrecte");
                    book = JsonIO.read(Book.class, new String(json, java.nio.charset.StandardCharsets.UTF_8));
                    if(!book.epoch.equals(identity.readString("UTF-8"))) throw new IOException("identité du journal incohérente");
                    if(book.version != 1 || book.pending.size > maxPending) throw new IOException("version/taille de journal invalide");
                }
                recover();
            }
        }catch(Throwable e){fail(e);throw new TransportException(failure);}
    }
    /** Sync the replacement before an atomic same-directory rename. Directory fsync is not portable on API 21.
     * No fallback to an older journal: that could resurrect goods which have already been delivered.
     */
    private static void persist(){
        try{
            journal.parent().mkdirs();
            if(!identity.exists()){
                try(var out = new FileOutputStream(identity.file())){out.write(book.epoch.getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}
            }
            byte[] bytes = JsonIO.write(book).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            CRC32 crc = new CRC32();crc.update(bytes);Fi temporary = journal.sibling(journal.name() + ".new");
            try(FileOutputStream raw = new FileOutputStream(temporary.file());DataOutputStream out = new DataOutputStream(raw)){
                out.writeInt(magic);out.writeInt(bytes.length);out.writeLong(crc.getValue());out.write(bytes);out.flush();raw.getFD().sync();
            }
            if(!temporary.file().renameTo(journal.file())) throw new IOException("remplacement du journal impossible");
        }catch(Throwable e){fail(e);throw new TransportException(failure);}
    }
    /** Read only canonical files, without SaveIO's automatic backup fallback. Check the entire compressed stream. */
    private static Rules durableRules(Sector sector){
        Fi f = file(sector);
        if(f == null || !f.exists()) throw new TransportException("point de sauvegarde absent du secteur " + sector.id);
        try{
            try(var in = SaveIO.getStream(f)){byte[] buffer = new byte[8192];while(in.read(buffer) != -1){ /* verify zlib trailer */ }}
            return SaveIO.getMeta(SaveIO.getStream(f)).rules;
        }catch(Throwable e){throw new TransportException("sauvegarde illisible du secteur " + sector.id);}
    }
    private static void confirm(Shipment s){
        if(s.phase == Phase.prepared){s.phase = Phase.transit;totals(s.source).sent += s.total();}
        else{totals(s.destination).received += s.total();book.pending.remove(s, true);}
    }
    private static void verify(Checkpoint c, Rules rules){
        if(c.generation != null && !c.generation.equals(rules.tags.get(generationTag)))
            throw new TransportException("génération de sauvegarde différente du journal, secteur " + c.sector);
        if(mark(rules, outTag) < c.outgoing || mark(rules, inTag) < c.incoming)
            throw new TransportException("sauvegarde antérieure aux transports confirmés du secteur " + c.sector);
    }
    private static void recover(){
        // Only interrupted operations require reading a world. No world is loaded or simulated.
        IntMap<Rules> rules = new IntMap<>();
        for(var s : book.pending){
            int id = s.phase == Phase.prepared ? s.source : s.destination;
            if(s.phase == Phase.transit || s.phase == Phase.overlay) continue;
            if(!rules.containsKey(id)) rules.put(id, durableRules(sector(id)));
        }
        for(var entry : rules){var c = checkpoint(entry.key);verify(c, entry.value);if(c.generation == null)c.generation = entry.value.tags.get(generationTag);}
        for(var s : book.pending.copy()){
            if(s.phase == Phase.prepared){
                var r = rules.get(s.source);verify(checkpoint(s.source), r);
                if(mark(r, outTag) >= s.sequence){
                    checkpoint(s.source).outgoing = Math.max(checkpoint(s.source).outgoing, s.sequence);confirm(s);
                }else book.pending.remove(s, true); // Native checkpoint still owns the undebited source buffer.
            }else if(s.phase == Phase.credit || s.phase == Phase.arriving){
                var r = rules.get(s.destination);verify(checkpoint(s.destination), r);
                if(s.incoming > 0 && mark(r, inTag) >= s.incoming){
                    checkpoint(s.destination).incoming = Math.max(checkpoint(s.destination).incoming, s.incoming);confirm(s);
                }else{
                    s.incoming = 0;
                    if(boundArrival(s) != null){s.phase = Phase.arriving;}
                    else{s.phase = Phase.transit;s.position = -1;}
                }
            }
        }
        persist();
    }
    /** Called before the native launch mutates its buffer. Waiting for a checkpoint is intentional. */
    public static boolean launch(LaunchPadBuild pad){
        if(!active() || pad.team != state.rules.defaultTeam) return false;
        try{
            ensure();Sector dest = state.getSector().info.destination;
            if(dest == null || dest == state.getSector() || dest.planet != state.getPlanet() || !dest.hasBase() || book.pending.size >= maxPending) return false;
            if(pad.items.total() != pad.block.itemCapacity) return false;
            Shipment s = prepare(dest, consumer -> pad.items.each((item, amount) -> consumer.get(item, amount)));
            pad.items.clear();mark(outTag, s.sequence);
            return true;
        }catch(TransportException e){return false;}
    }
    private interface ItemsReader{void each(arc.func.Cons2<Item, Integer> consumer);}
    private static Shipment prepare(Sector dest, ItemsReader items){
        Shipment s = new Shipment();s.sequence = book.next++;s.id = book.epoch + ":" + s.sequence;
        s.source = state.getSector().id;s.destination = dest.id;
        items.each((item, amount) -> {if(amount > 0)s.cargo.add(new Cargo(item, amount));});
        book.pending.add(s);persist();return s;
    }
    /** Adopt only actual, serialized M7B flight stacks; statistical arrivals have no such proof. */
    public static boolean adopt(Seq<ItemStack> stacks){
        if(!active() || stacks.isEmpty()) return true;
        try{
            ensure();Sector dest = state.getSector().info.destination;
            if(dest == null || dest == state.getSector() || dest.planet != state.getPlanet() || !dest.hasBase() || book.pending.size >= maxPending) return false;
            Shipment s = prepare(dest, consumer -> {for(var stack : stacks)consumer.get(stack.item, stack.amount);});
            stacks.clear();mark(outTag, s.sequence);return true;
        }catch(TransportException e){return false;}
    }
    /** Invoked only by canonical native checkpoints, not manual exports of an unrelated file. */
    public static void beforeSave(Fi file){
        if(!canonical(file)) return;
        ensure();Checkpoint c = checkpoint(state.getSector().id);verify(c, state.rules);
        if(c.generation == null && !state.rules.tags.containsKey(generationTag)) state.rules.tags.put(generationTag, UUID.randomUUID().toString());
    }
    /** An explicitly regenerated native colony is a new world, not a rollback of a previous receipt. */
    public static void generated(Sector sector){
        if(!enabled(sector.planet)) return;
        ensure();recover();Checkpoint c = checkpoint(sector.id);
        c.generation = UUID.randomUUID().toString();c.receivers.clear();
        for(var s : book.pending){
            if(s.destination == sector.id && s.phase == Phase.overlay){s.phase = Phase.transit;s.incoming = 0;s.position = -1;}
        }
        state.rules.tags.put(generationTag, c.generation);mark(outTag, c.outgoing);mark(inTag, c.incoming);persist();
    }
    public static void afterSave(Fi file){
        if(!canonical(file)) return;
        try{
            ensure();try(var raw = new RandomAccessFile(file.file(), "rw")){raw.getFD().sync();}
            Sector sector = state.getSector();Checkpoint c = checkpoint(sector.id);
            c.generation = state.rules.tags.get(generationTag);
            c.outgoing = mark(state.rules, outTag);c.incoming = mark(state.rules, inTag);
            for(var s : book.pending.copy()){
                if(s.source == sector.id && s.phase == Phase.prepared && s.sequence <= c.outgoing) confirm(s);
                else if(s.destination == sector.id && s.incoming > 0 && s.incoming <= c.incoming) confirm(s);
            }
            c.receivers.clear();
            for(var building : state.rules.defaultTeam.data().buildings){
                if(building instanceof LandingPadBuild pad){
                    LandingPad type = (LandingPad)pad.block;Receiver r = new Receiver();r.position = pad.pos();r.block = type.name;
                    r.config = pad.config == null ? null : pad.config.name;r.amount = pad.items.total();r.capacity = type.itemCapacity;
                    r.water = pad.liquids.get(type.consumeLiquid);r.required = type.consumeLiquidAmount;r.enabled = pad.enabled;
                    r.busy = pad.arriving != null;r.arrivalId = pad.transportId;
                    r.arrivalItem = pad.arriving == null ? null : pad.arriving.name;
                    r.paidWater = pad.arriving == null ? 0f : pad.liquidRemoved;c.receivers.add(r);
                }
            }
            persist();
        }catch(Throwable e){fail(e);throw new TransportException(failure);}
    }
    /** Runs after the native file load, outside SaveIO's corruption/fallback catch. Never delete a world on ambiguity. */
    public static void loaded(){
        if(!active()) return;
        try{
            ensure();Sector sector = state.getSector();Checkpoint c = checkpoint(sector.id);verify(c, state.rules);
            if(book.next == 1 && (mark(state.rules, outTag) != 0 || mark(state.rules, inTag) != 0))
                throw new TransportException("journal absent pour une sauvegarde déjà engagée dans les transports");
            long loadedIncoming = mark(state.rules, inTag);
            // Validate every overlay before touching any inventory. Baseline changed => quarantine, no guessed credit.
            for(var s : book.pending){
                if(s.destination != sector.id || s.phase != Phase.overlay || s.incoming <= loadedIncoming) continue;
                var r = c.receivers.find(p -> p.position == s.position);var build = world.build(s.position);
                if(r == null || !(build instanceof LandingPadBuild pad) || !pad.block.name.equals(r.block) ||
                    !Objects.equals(pad.config == null ? null : pad.config.name, r.config) || pad.items.total() != 0 ||
                    (s.resumedArrival ? !matchesSavedArrival(s, r, pad) : pad.arriving != null) ||
                    Math.abs(pad.liquids.get(((LandingPad)pad.block).consumeLiquid) - s.waterBefore) > 0.01f)
                    throw new TransportException("tampon de réception hors écran modifié, secteur " + sector.id);
            }
            for(var s : book.pending){
                if(s.destination != sector.id || s.phase != Phase.overlay || s.incoming <= loadedIncoming) continue;
                LandingPadBuild pad = (LandingPadBuild)world.build(s.position);LandingPad type = (LandingPad)pad.block;
                for(var cargo : s.cargo)pad.items.add(content.item(cargo.item), cargo.amount);
                pad.liquids.remove(type.consumeLiquid, s.waterCost < 0f ? type.consumeLiquidAmount : s.waterCost);
                if(s.resumedArrival){pad.arriving = null;pad.arrivingTimer = 0f;pad.liquidRemoved = 0f;pad.transportId = null;}
                pad.cooldown = 1f;
                mark(inTag, Math.max(mark(state.rules, inTag), s.incoming));
            }
            // A saved landing animation can outlive a journal replay; a finite transit manifest is still required.
            for(var build : state.rules.defaultTeam.data().buildings){
                if(build instanceof LandingPadBuild pad && pad.transportId != null){
                    var s = book.pending.find(t -> t.id.equals(pad.transportId));
                    if(s != null && s.phase == Phase.transit && s.destination == sector.id){s.phase = Phase.arriving;s.position = pad.pos();}
                    else if(s == null || s.phase != Phase.arriving){pad.transportId = null;pad.arriving = null;pad.arrivingTimer = 0;}
                }
            }
        }catch(Throwable e){fail(e);state.set(mindustry.core.GameState.State.menu);throw new TransportException(failure);}
    }
    public static void receive(LandingPadBuild pad){
        if(!active() || pad.isFake()) return;
        try{
            ensure();
            if(pad.transportId != null || pad.arriving != null || pad.config == null || pad.items.total() != 0 || pad.cooldown > 0 || pad.efficiency <= 0) return;
            var s = book.pending.find(t -> t.phase == Phase.transit && t.destination == state.getSector().id && matches(t, pad.config, pad.block.itemCapacity));
            if(s == null) return;
            s.phase = Phase.arriving;s.position = pad.pos();persist();pad.transportId = s.id;pad.handleLanding();
        }catch(TransportException ignored){}
    }
    private static boolean matches(Shipment s, Item item, int amount){return item != null && s.cargo.size == 1 && s.cargo.first().item.equals(item.name) && s.cargo.first().amount == amount;}
    /** Before the native animation credits the pad; unknown old statistical animations stay suspended. */
    public static boolean canArrive(LandingPadBuild pad){
        try{
            ensure();var s = book.pending.find(t -> t.id.equals(pad.transportId));
            return s != null && s.phase == Phase.arriving && s.destination == state.getSector().id && s.position == pad.pos() && matches(s, pad.arriving, pad.block.itemCapacity);
        }catch(TransportException e){return false;}
    }
    public static boolean credit(LandingPadBuild pad){
        if(!canArrive(pad) || pad.items.total() != 0) return false;
        try{
            var s = book.pending.find(t -> t.id.equals(pad.transportId));s.incoming = book.next++;s.phase = Phase.credit;persist();
            mark(inTag, s.incoming);pad.transportId = null;return true;
        }catch(TransportException e){return false;}
    }
    /** A journal claim alone is not proof that the arrival was written into the receiver's world. */
    private static Receiver boundArrival(Shipment s){
        var c = book.checkpoints.find(w -> w.sector == s.destination);
        if(c == null) return null;
        return c.receivers.find(r -> r.position == s.position && r.busy && s.id.equals(r.arrivalId) &&
            r.arrivalItem != null && matches(s, content.item(r.arrivalItem), r.capacity) &&
            Float.isFinite(r.paidWater) && r.paidWater >= 0f && r.paidWater <= r.required + 0.01f);
    }
    private static boolean matchesSavedArrival(Shipment s, Receiver r, LandingPadBuild pad){
        return s.id.equals(pad.transportId) && pad.arriving != null && pad.arriving.name.equals(r.arrivalItem) &&
            Math.abs(pad.liquidRemoved - r.paidWater) <= 0.01f;
    }
    private static void reserve(Shipment s, Receiver r, boolean resumed){
        s.phase = Phase.overlay;s.position = r.position;s.incoming = book.next++;s.waterBefore = r.water;
        s.resumedArrival = resumed;s.waterCost = Math.max(0f, r.required - (resumed ? r.paidWater : 0f));
        r.amount = r.capacity;r.water -= s.waterCost;r.busy = true;
    }
    /** At most one reserved lot per known empty receiver. No offscreen conveyor, recipe, fuel or core extrapolation. */
    public static void turn(Planet planet){
        if(!enabled(planet) || net.client()) return;
        try{
            ensure();boolean changed = false;
            for(var s : book.pending){
                if(s.phase == Phase.arriving && sector(s.destination).isBeingPlayed()){
                    var build = world.build(s.position);
                    if(!(build instanceof LandingPadBuild pad) || !s.id.equals(pad.transportId)){
                        s.phase = Phase.transit;s.position = -1;changed = true;
                    }
                }
            }
            for(var c : book.checkpoints){
                Sector destination = sector(c.sector);
                if(destination.isBeingPlayed() || !destination.hasBase()) continue;
                for(var r : c.receivers){
                    if(!r.enabled || r.amount != 0) continue;
                    if(r.busy){
                        var s = book.pending.find(t -> t.phase == Phase.arriving && t.destination == c.sector && boundArrival(t) == r);
                        if(s == null || r.water < Math.max(0f, r.required - r.paidWater)) continue;
                        reserve(s, r, true);changed = true;
                    }else{
                        if(r.config == null || r.water < r.required) continue;
                        var s = book.pending.find(t -> t.phase == Phase.transit && t.destination == c.sector && matches(t, content.item(r.config), r.capacity));
                        if(s == null) continue;
                        reserve(s, r, false);changed = true;
                    }
                }
            }
            if(changed) persist();
        }catch(TransportException ignored){}
    }
    private static boolean applied(Shipment s){
        return s.phase == Phase.overlay && active() && state.getSector().id == s.destination && mark(state.rules, inTag) >= s.incoming;
    }
    private static String phaseName(Shipment s){
        if(applied(s)) return "tampon appliqué, crédit à sauvegarder";
        return switch(s.phase){
            case prepared -> "débit à sauvegarder";
            case transit -> "attente de réception";
            case arriving -> "atterrissage";
            case credit -> "crédit à sauvegarder";
            case overlay -> "tampon réservé hors écran, à confirmer";
        };
    }
    private static String waiting(Shipment s){
        Sector destination = sector(s.destination);
        if(!destination.hasBase()) return "destination sans base";
        if(destination.isBeingPlayed()) return "réception active : vérifier configuration, eau et sortie";
        var c = book.checkpoints.find(w -> w.sector == s.destination);
        if(c == null) return "destination non relevée depuis M7C : visiter et sauvegarder une fois";
        if(c.receivers.isEmpty()) return "aucune réception enregistrée à destination";
        if(s.phase == Phase.arriving){
            var r = boundArrival(s);
            if(r == null) return "ancienne arrivée non appariée : visiter et sauvegarder pour relever son état";
            if(!r.enabled) return "réception désactivée";
            return r.water < Math.max(0f, r.required - r.paidWater) ? "eau insuffisante pour terminer l'arrivée" : "fin au prochain tour économique";
        }
        var pads = c.receivers.select(r -> r.config != null && matches(s, content.item(r.config), r.capacity));
        if(pads.isEmpty()) return "aucune réception configurée pour cette matière";
        if(!pads.contains(r -> r.enabled)) return "réceptions désactivées";
        if(!pads.contains(r -> r.enabled && r.amount == 0)) return "tampons occupés";
        if(!pads.contains(r -> r.enabled && r.amount == 0 && !r.busy)) return "arrivée déjà engagée ou ancien relevé : visiter si nécessaire";
        if(!pads.contains(r -> r.enabled && r.amount == 0 && !r.busy && r.water >= r.required)) return "eau insuffisante";
        return "prochain tour économique (120 secondes de jeu, hors pause)";
    }
    /** Detached diagnostics. Reading does not initialize, recover, write or refresh the engine. */
    public static String describe(Sector sector){
        if(!enabled(sector.planet)) return "";
        if(session != Core.settings || book == null) return "Transports conservatifs : journal pas encore ouvert.";
        if(failure != null) return failure;
        long pending = 0, transit = 0, receiving = 0, virtual = 0, arriving = 0;int lots = 0;
        for(var s : book.pending){
            if(s.source != sector.id && s.destination != sector.id) continue;
            lots++;if(s.phase == Phase.prepared) pending += s.total();
            else if(s.phase == Phase.credit || applied(s)) receiving += s.total();
            else if(s.phase == Phase.overlay) virtual += s.total();
            else if(s.phase == Phase.arriving) arriving += s.total();else transit += s.total();
        }
        Totals totals = book.totals.find(t -> t.sector == sector.id);
        StringBuilder detail = new StringBuilder();int shown = 0;
        for(var s : book.pending){
            if(s.source != sector.id && s.destination != sector.id) continue;
            if(shown++ >= 12){detail.append("\nAutres lots : consulter leur secteur source/destination.");break;}
            detail.append("\n#").append(s.sequence).append(" · ").append(s.source).append(" → ").append(s.destination).append(" · ").append(phaseName(s));
            for(var cargo : s.cargo)detail.append(" · ").append(cargo.item).append(" : ").append(cargo.amount);
            if(s.phase == Phase.transit || s.phase == Phase.arriving) detail.append("\n").append(waiting(s));
        }
        return "Mouvements enregistrés (objets, pas débits moyens)\nExpédiés confirmés : " + (totals == null ? 0 : totals.sent) +
            "\nReçus confirmés : " + (totals == null ? 0 : totals.received) + " (monde destinataire sauvegardé)" + "\nDébit en attente de sauvegarde : " + pending +
            "\nEn transit / attente de tampon : " + transit + "\nRéservés hors écran : " + virtual + "\nAtterrissages en cours : " + arriving + "\nCrédit en attente de sauvegarde : " + receiving + "\nLots non soldés : " + lots +
            "\nHors écran : lots réservés dans les tampons connus, appliqués au chargement du monde ; aucun crédit de noyau avant visite. Un tour = 120 secondes de jeu ; carte et diagnostic mettent en pause en solo. Sans convoyeurs ni recettes hors écran. Journal plafonné à 512 lots ; les rampes attendent ensuite." + detail;
    }
}
