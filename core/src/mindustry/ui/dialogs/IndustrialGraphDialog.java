package mindustry.ui.dialogs;

import arc.*;
import arc.math.geom.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.IndustrialEconomyCapture.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;

import java.util.*;

import static mindustry.Vars.*;

/** Paginated detached inspection: no polling of native worlds and no simulation controls. */
public class IndustrialGraphDialog extends BaseDialog{
    private Sector sector;
    private Snapshot snapshot;
    private int selected = -1, page;
    private String query = "";
    private Label age;
    private long clock;
    private boolean unavailable;
    private static final int pageSize = 30;

    public IndustrialGraphDialog(){
        super("Graphe économique — capture", Styles.fullDialog);
        shouldPause = true;addCloseListener();
        shown(() -> {snapshot = IndustrialEconomyCapture.latest(sector);rebuild();});
        onResize(this::rebuild);
        hidden(() -> {snapshot = null;sector = null;age = null;selected = -1;page = 0;query = "";unavailable = false;cont.clear();});
        update(() -> {if(age != null && Time.timeSinceMillis(clock) >= 1000){clock = Time.millis();age.setText(age());}});
    }
    public void show(Sector sector){
        if(sector == null || !IndustrialEconomyCapture.enabled(sector.planet))return;
        this.sector = sector;show();
    }
    private float width(){return Math.min(460f, Math.max(120f, Core.graphics.getWidth() / Scl.scl(1f) - 48f));}
    private void line(Table t, String s){t.add(s).left().wrap().growX().pad(5).row();}
    private String number(double value){return Double.isFinite(value) ? String.format(Locale.ROOT, value == Math.rint(value) ? "%.0f" : "%.3f", value) : "INVALIDE";}
    private String item(int id){return id >= 0 && id < snapshot.itemNames.length ? snapshot.itemNames[id] : "inconnu#" + id;}
    private String liquid(int id){return id >= 0 && id < snapshot.liquidNames.length ? snapshot.liquidNames[id] : "inconnu#" + id;}
    private String age(){return snapshot == null ? "Aucun relevé." : "Relevé de session, " + snapshot.origin + ", il y a " + Math.max(0L, (Time.nanos() - snapshot.nanos) / 1000000000L) + " s.\nUn relevé inactif n'est pas un stock actuel ni une prévision.";}
    private String name(Node n){return snapshot.specs.get(n.spec).name + " — " + Point2.x(n.position) + ", " + Point2.y(n.position);}
    private void back(){if(selected >= 0){selected = -1;rebuild();}else hide();}
    private void capture(){
        if(!IndustrialEconomyCapture.current(sector)) return;
        unavailable = IndustrialEconomyCapture.observe(Origin.manual) == null;
        snapshot = IndustrialEconomyCapture.latest(sector);page = 0;selected = -1;rebuild();
    }
    private void rebuild(){
        cont.clear();buttons.clear();
        float w = Math.min(180f, (width() - 8f) / 2f);
        buttons.button("@back", Icon.left, this::back).name("graph-back").width(w).height(64);
        buttons.button("Capturer", this::capture).disabled(b -> !IndustrialEconomyCapture.current(sector)).name("graph-capture").width(w).height(64);
        Table body = new Table();body.defaults().growX();
        line(body, sector.planet.localizedName + " / " + sector.name());
        line(body, "LECTURE SEULE — aucune simulation autonome activée. Aucun stock, paiement ou transport déclenché par cet écran.");
        age = body.add(age()).left().wrap().growX().pad(5).get();body.row();clock = Time.millis();
        if(unavailable)line(body, "Capture indisponible ; le jeu n'a pas été modifié.");
        if(snapshot == null){
            line(body, "Secteur non capturé ou relevé évincé du cache. Visiter ce secteur pour un relevé automatique au chargement/sauvegarde, ou le capturer lorsqu'il est actif.");
            line(body, "Cache en mémoire seulement : les relevés ne survivent pas au redémarrage. Aucun monde inactif n'est chargé pour les reconstituer.");
        }else if(selected >= 0 && selected < snapshot.nodes.size){node(body, selected);}
        else{overview(body);}
        cont.pane(body).width(width()).growY().scrollX(false).name("graph-scroll");
    }
    private void overview(Table body){
        line(body, snapshot.candidate ? "Candidat structurel au prototype minimal M7D-A. Ceci n'active aucun moteur et ne garantit pas le fonctionnement d'une usine." : "NON ÉLIGIBLE au prototype minimal M7D-A — motifs ci-dessous.");
        line(body, "Bâtiments : " + snapshot.nodes.size + "/" + snapshot.nativeBuildings + " ; liaisons : " + snapshot.links.size + " ; réseaux : " + snapshot.circuits.size);
        line(body, "Relevé " + (snapshot.complete ? "complet dans le périmètre décodé" : "PARTIEL") + " ; inventaires " + (snapshot.stocksComplete ? "modules et cargaisons recensés" : "INCOMPLETS") + ".");
        line(body, "Modules uniques d'objets : " + snapshot.itemStores.size + " ; liquides : " + snapshot.liquidStores.size + ". Les modules partagés sont comptés une fois.");
        for(var issue : snapshot.issues)line(body, (issue.blocking ? "Blocage : " : "Observation : ") + issue.detail + (issue.block == null ? "" : " — " + issue.block) + " (" + issue.count + ")");
        line(body, "Stocks observés, sans cargaisons du registre ajoutées une deuxième fois :");
        boolean any = false;
        for(int i = 0; i < snapshot.itemTotals.length; i++)if(snapshot.itemTotals[i] != 0){line(body, item(i) + " : " + snapshot.itemTotals[i]);any = true;}
        for(int i = 0; i < snapshot.liquidTotals.length; i++)if(snapshot.liquidTotals[i] != 0){line(body, liquid(i) + " : " + number(snapshot.liquidTotals[i]));any = true;}
        if(!any)line(body, "Aucun objet/liquide recensé.");
        line(body, "Énergie réellement stockée en batteries : " + number(snapshot.energy) + " unités natives. Le statut d'un consommateur n'est pas une batterie.");
        for(int i = 0; i < Math.min(snapshot.circuits.size, 20); i++){
            Circuit c = snapshot.circuits.get(i);line(body, "Réseau #" + i + " · " + c.members.size + " membres · batterie " + number(c.energy) + " ; dernière observation native entrée/sortie : " + number(c.observedInPerTick) + "/" + number(c.observedOutPerTick) + " par tick (pas une réserve).");
        }
        line(body, "Références de manifestes M7C.1 : " + snapshot.manifests.size + " ; jamais additionnées à ces stocks. Ne pas sommer des relevés de dates différentes.");
        for(var m : snapshot.manifests){
            String text = m.source + " → " + m.destination + " · " + m.phase + " · " + m.ownership;
            for(int i = 0; i < m.items.length; i++)text += " · " + m.items[i] + " : " + m.amounts[i];
            line(body, text);
        }
        line(body, "Empreinte structure/configuration : " + snapshot.topology + "\nCapture : " + number(snapshot.captureNanos / 1000000d) + " ms ; cache borné à 100 secteurs / 100 000 nœuds, en mémoire de session.");
        TextField search = new TextField(query);search.setMessageText("Filtrer les bâtiments");Table list = new Table();
        search.changed(() -> {query = search.getText();page = 0;nodes(list);});
        body.add(search).growX().pad(5).row();body.add(list).growX().row();nodes(list);
    }
    private void nodes(Table list){
        list.clear();list.defaults().growX();String filter = query.toLowerCase(Locale.ROOT);
        int total = snapshot.nodes.count(n -> name(n).toLowerCase(Locale.ROOT).contains(filter));
        int pages = Math.max(1, (total + pageSize - 1) / pageSize);page = Math.min(page, pages - 1);
        Table nav = new Table();nav.button("‹", () -> {page--;nodes(list);}).disabled(page == 0).height(60).width(70);
        nav.add(" " + (page + 1) + "/" + pages + " ").growX();nav.button("›", () -> {page++;nodes(list);}).disabled(page + 1 >= pages).height(60).width(70);
        list.add(nav).growX().row();int row = 0;
        for(int i = 0; i < snapshot.nodes.size; i++){
            Node n = snapshot.nodes.get(i);if(!name(n).toLowerCase(Locale.ROOT).contains(filter))continue;
            if(row++ < page * pageSize || row > (page + 1) * pageSize)continue;
            final int index = i;
            list.button(name(n), Styles.flatt, () -> {selected = index;rebuild();}).name("graph-node-" + n.position).with(b -> b.getLabel().setWrap(true)).minHeight(60).growX().pad(3).row();
        }
    }
    private void node(Table body, int index){
        Node n = snapshot.nodes.get(index);Spec spec = snapshot.specs.get(n.spec);
        line(body, name(n));line(body, "Type : " + spec.type + " ; " + (spec.supported ? "dans le périmètre structurel initial" : "non pris en charge pour la future simulation"));
        line(body, "Activé : " + n.enabled + " ; efficacité observée : " + number(n.efficiency) + " ; cadence : " + number(n.timeScale));
        line(body, "Capacité native déclarée d'objets : " + spec.itemCapacity + " ; noyau par matière : " + n.coreCapacity + " ; liquides : " + number(spec.liquidCapacity));
        if(n.itemStore >= 0){
            ItemStore store = snapshot.itemStores.get(n.itemStore);line(body, "Stock d'objets #" + n.itemStore + " partagé par " + store.members.size + " bâtiment(s), compté UNE fois dans le total.");
            for(int i = 0; i < store.ids.size; i++)line(body, item(store.ids.get(i)) + " : " + store.amounts.get(i));
        }
        if(n.liquidStore >= 0){LiquidStore store = snapshot.liquidStores.get(n.liquidStore);line(body, "Module liquide #" + n.liquidStore + " ; propriétaires : " + store.members.size);for(int i = 0; i < store.ids.size; i++)line(body, liquid(store.ids.get(i)) + " : " + number(store.amounts.get(i)));}
        if(n.powerStore >= 0){PowerStore p = snapshot.powerStores.get(n.powerStore);line(body, "Réseau #" + n.circuit + " ; statut : " + number(p.status) + (p.buffered ? " ; batterie capacité " + number(p.capacity) : " ; couverture de demande, aucune énergie stockée"));}
        for(var e : n.tags)line(body, e.key + " : " + e.value);
        for(var e : n.phase)line(body, e.key + " : " + number(e.value));
        if(!n.stateDecoded)line(body, "État spécifique non décodé : ne pas utiliser ce relevé pour simuler ce bâtiment.");
        if(n.conveyorIds.length > 0){line(body, "Objets internes du convoyeur (miroir du module, non additionnés) :");for(int i = 0; i < n.conveyorIds.length; i++)line(body, item(n.conveyorIds[i]) + " : x=" + number(n.conveyorX[i]) + " y=" + number(n.conveyorY[i]));}
        line(body, "Recettes/coûts de base natifs, pas des consommations exécutées :");
        for(Consumer c : spec.consumers){
            line(body, c.type + " ; continu : " + c.continuous + " ; optionnel : " + c.optional + " ; boost : " + c.booster);
            for(int i = 0; i < c.itemIds.size; i++)line(body, item(c.itemIds.get(i)) + " × " + c.amounts.get(i));
            if(!c.known)line(body, "Consommateur dynamique non décodé ; aucune dépense calculée.");
            if(c.liquid != null)line(body, c.liquid + " : " + (c.continuous ? number(c.liquidPerTick * 60d) + "/s de base" : number(c.liquidAmount) + " quantité/seuil natif, contrôle manuel du bloc"));
            if(c.powerPerTick > 0)line(body, "Demande nominale : " + number(c.powerPerTick) + " unités/tick");
        }
        for(int i = 0; i < spec.outputItems.size; i++)line(body, "Sortie par cycle : " + item(spec.outputItems.get(i)) + " × " + spec.outputAmounts.get(i));
        for(int i = 0; i < spec.outputLiquids.size; i++)line(body, "Sortie liquide : " + spec.outputLiquids.get(i) + " · " + number(spec.liquidAmounts.get(i)) + "/tick");
        line(body, "Liaisons : 'adjacency' = proximité seulement. 'items/liquid/power' marquent un type ; sans support explicite, aucun transfert n'est prouvé. Aucun débit n'est exécuté.");
        int shown = 0;
        for(Link l : snapshot.links){
            if(l.from != index && l.to != index)continue;
            if(shown++ == 100){line(body, "Détail limité aux 100 premières liaisons ; le relevé reste conservé.");break;}
            line(body, name(snapshot.nodes.get(l.from)) + " → " + name(snapshot.nodes.get(l.to)) + " · " + l.kind + " · " + (l.supported ? "structure reconnue" : "non validée"));
        }
    }
}
