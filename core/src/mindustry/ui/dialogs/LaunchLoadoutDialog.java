package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SchematicsDialog.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Dialog for selecting loadout at sector launch. */
public class LaunchLoadoutDialog extends BaseDialog{
    LoadoutDialog loadout = new LoadoutDialog();
    //total required items
    ItemSeq total = new ItemSeq();
    //currently selected schematic
    Schematic selected;
    //validity of loadout items
    boolean valid;
    //last calculated capacity
    int lastCapacity;

    public LaunchLoadoutDialog(){
        super("@configure");
    }

    public void show(CoreBlock core, Sector sector, Sector destination, Runnable confirm){
        cont.clear();
        buttons.clear();

        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide);

        addCloseListener();

        boolean expansion = IndustrialCampaign.strict(destination.planet);
        if(expansion) IndustrialExpansion.recover(destination.planet);
        ItemSeq sitems = sector.items();
        ItemSeq surcharge = IndustrialExpansion.surcharge(destination);

        //hide nonsensical items
        ItemSeq launch = universe.getLaunchResources();
        if(sector.planet.allowLaunchLoadout){
            for(var item : content.items()){
                if(!item.isOnPlanet(sector.planet)){
                    launch.set(item, 0);
                }
            }
            universe.updateLaunchResources(launch);
        }

        //updates sum requirements
        Runnable update = () -> {
            int cap = lastCapacity = (int)(sector.planet.launchCapacityMultiplier * selected.findCore().itemCapacity);

            //cap resources based on core type
            ItemSeq schems = selected.requirements();
            ItemSeq resources = universe.getLaunchResources();
            resources.min(cap);

            int capacity = lastCapacity;

            if(!destination.allowLaunchLoadout()){
                resources.clear();
                if(destination.preset != null){
                    var rules = destination.preset.generator.map.rules();
                    for(var stack : rules.loadout){
                        if(stack.item.isOnPlanet(sector.planet)){
                            resources.add(stack.item, stack.amount);
                        }
                    }
                }

            }else if(getMax()){
                for(Item item : content.items()){
                    resources.set(item, Mathf.clamp(sitems.get(item) - schems.get(item) - surcharge.get(item), 0, capacity));
                }
            }

            universe.updateLaunchResources(resources);

            total.clear();
            selected.requirements().each(total::add);
            universe.getLaunchResources().each(total::add);
            surcharge.each(total::add);
            valid = sitems.has(total) || PlanetDialog.debugSelect;
        };

        Cons<Table> rebuild = table -> {
            table.clearChildren();
            int i = 0;

            ItemSeq schems = selected.requirements();
            ItemSeq launches = universe.getLaunchResources();

            for(ItemStack s : total){
                int as = schems.get(s.item), al = launches.get(s.item), fee = surcharge.get(s.item);

                if(as + al + fee == 0) continue;
                if(expansion){
                    table.table(card -> {
                        card.image(s.item.uiIcon).size(iconSmall).padRight(8);
                        card.add(s.item.localizedName).left().growX();
                        card.row();
                        card.add(Core.bundle.format("industrial.expansion.breakdown", as, al, fee)).colspan(2).left().wrap().growX();
                        card.row();
                        card.add(Core.bundle.format("industrial.expansion.balance", s.amount, sitems.get(s.item)))
                            .color(sitems.has(s.item, s.amount) ? arc.graphics.Color.white : arc.graphics.Color.scarlet)
                            .colspan(2).left().wrap().growX();
                    }).growX().pad(6).row();
                    continue;
                }

                table.image(s.item.uiIcon).left().size(iconSmall);

                String amountStr = (al + as) + (destination.allowLaunchLoadout() ? "[gray] (" + (al + " + " + as + ")") : "");

                table.add(
                    sitems.has(s.item, s.amount) ? amountStr :
                    "[scarlet]" + (Math.min(sitems.get(s.item), s.amount) + "[lightgray]/" + amountStr)).padLeft(2).left().padRight(4);

                if(++i % 4 == 0){
                    table.row();
                }
            }
        };

        Table items = new Table();

        Runnable rebuildItems = () -> rebuild.get(items);

        if(destination.allowLaunchLoadout()){
            buttons.button("@resources.max", Icon.add, Styles.togglet, () -> {
                setMax(!getMax());
                update.run();
                rebuildItems.run();
            }).checked(b -> getMax());

            buttons.button("@resources", Icon.edit, () -> {
                ItemSeq stacks = universe.getLaunchResources();
                Seq<ItemStack> out = stacks.toSeq();

                ItemSeq realItems = sitems.copy();
                selected.requirements().each(realItems::remove);
                if(expansion){
                    surcharge.each(realItems::remove);
                    realItems.checkNegative();
                }

                loadout.show(lastCapacity, realItems, out, i -> (IndustrialCampaign.isPlanet(sector.planet) || i.unlocked()) && i.isOnPlanet(sector.planet), out::clear, () -> {}, () -> {
                    universe.updateLaunchResources(new ItemSeq(out));
                    update.run();
                    rebuildItems.run();
                });
            }).disabled(b -> getMax());
        }

        boolean rows = Core.graphics.isPortrait() && mobile;

        if(rows) buttons.row();

        var cell = buttons.button("@launch.text", Icon.ok, () -> {
            if(!expansion){
                universe.updateLoadout(core, selected);
                confirm.run();
                hide();
                return;
            }
            try{
                // Revalidate actual source stock at confirmation; the dialog may have remained open.
                if(expansion && !sector.items().has(total)){
                    sitems.clear(); sector.items().each(sitems::add);
                    update.run(); rebuildItems.run(); return;
                }
                universe.updateLoadout(core, selected);
                confirm.run();
                hide();
            }catch(Throwable failure){
                Log.err(failure);
                ui.showException(Core.bundle.get("industrial.expansion.error"), failure);
            }
        }).name("launch-sector-confirm").disabled(b -> !valid);

        if(rows){
            cell.colspan(2).size(160f + 160f + 4f, 64f);
        }

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        ButtonGroup<Button> group = new ButtonGroup<>();
        selected = universe.getLoadout(core);
        if(selected == null) selected = schematics.getLoadouts().get((CoreBlock)Blocks.coreShard).first();
        if(IndustrialCampaign.strict(destination.planet) && selected.tiles.contains(tile -> !IndustrialResearch.available(tile.block, destination.planet))) selected = mindustry.content.Loadouts.basicShard;

        cont.add(Core.bundle.format("launch.from", sector.name())).row();
        if(expansion){
            var ledger = IndustrialExpansion.ledger(destination.planet);
            cont.add(Core.bundle.format("industrial.expansion.destination", destination.name())).name("industrial-expansion-destination").wrap().growX().row();
            cont.add(Core.bundle.format("industrial.expansion.counter", ledger.established + 1)).wrap().growX().row();
            if(ledger.grandfathered > 0) cont.add("@industrial.expansion.legacy").wrap().growX().pad(4).row();
        }

        if(destination.allowLaunchSchematics()){
            cont.pane(t -> {
                int[] i = {0};

                Cons<Schematic> handler = s -> {
                    if(IndustrialCampaign.strict(destination.planet) && s.tiles.contains(tile -> !IndustrialResearch.available(tile.block, destination.planet))) return;
                    if(IndustrialCampaign.isPlanet(destination.planet) && s.tiles.contains(tile -> !IndustrialContent.industrial(tile.block))) return;
                    if(s.tiles.contains(tile -> !tile.block.supportsEnv(sector.planet.defaultEnv) ||
                    //make sure block can be built here.
                    !tile.block.isOnPlanet(sector.planet))){
                        return;
                    }

                    t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> {
                        selected = s;
                        update.run();
                        rebuildItems.run();
                    }).group(group).pad(4).checked(s == selected).size(200f);

                    if(++i[0] % cols == 0){
                        t.row();
                    }
                };

                if(destination.allowLaunchSchematics() || schematics.getDefaultLoadout(core) == null){
                    for(var entry : schematics.getLoadouts()){
                        if(entry.key.size <= core.size){
                            for(Schematic s : entry.value){
                                handler.get(s);
                            }
                        }
                    }
                }else{
                    //only allow launching with the standard loadout schematic
                    handler.get(schematics.getDefaultLoadout(core));
                }
            }).growX().scrollX(false);

            cont.row();

            cont.label(() -> Core.bundle.format("launch.capacity", lastCapacity)).row();
            cont.row();
        }else if(destination.preset != null && destination.preset.description != null){
            cont.pane(p -> {
                p.add(destination.preset.description).grow().wrap().labelAlign(Align.center);
            }).pad(10f).grow().row();
        }

        if(expansion){cont.pane(items).growX().scrollX(false);}else{cont.pane(items);}
        cont.row();
        cont.add("@sector.missingresources").visible(() -> !valid);

        update.run();
        rebuildItems.run();

        show();
    }

    void setMax(boolean max){
        Core.settings.put("maxresources", max);
    }

    boolean getMax(){
        return Core.settings.getBool("maxresources", true);
    }
}
