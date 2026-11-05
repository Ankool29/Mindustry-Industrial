package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.layout.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.IndustrialResearch.*;
import mindustry.gen.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

/** Native dialog controls with a scrolling portrait sheet for a detached research node. */
public class IndustrialResearchDetailsDialog extends BaseDialog{
    private final IndustrialNode node;
    private final Runnable changed;

    public IndustrialResearchDetailsDialog(IndustrialNode node, Runnable changed){
        super(node.content.localizedName);
        this.node = node;
        this.changed = changed;
        shouldPause = true;
        addCloseButton();
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    private boolean canPay(){
        if(IndustrialResearch.unrestricted(node.planet) || net.client() || !IndustrialResearch.canResearch(node)) return false;
        if(node.requirements.length == 0) return true;
        var items = IndustrialResearch.availableItems(node.planet);
        for(int i = 0; i < node.requirements.length; i++){
            if(node.finishedRequirements[i].amount < node.requirements[i].amount && items.get(node.requirements[i].item) > 0) return true;
        }
        return false;
    }

    private void open(IndustrialNode other){
        hide();
        new IndustrialResearchDetailsDialog(other, changed).show();
    }

    private void rebuild(){
        node.refresh();
        cont.clear();
        Table body = new Table();
        float width = Math.min(450f, Core.graphics.getWidth() / Scl.scl(1f) - 48f);
        body.defaults().width(width).left().pad(5);
        body.table(header -> {
            header.image(node.content.uiIcon).size(48).padRight(12);
            header.add(node.content.localizedName).growX().wrap();
            header.button(Icon.info, () -> ui.content.showIndustrial(node.content)).size(48);
        }).row();
        boolean free = IndustrialResearch.unrestricted(node.planet);
        boolean done = IndustrialResearch.available(node.content, node.planet);
        body.add(free ? "@industrial.research.free" : done ? "@completed" : "@industrial.research.locked").wrap().color(Pal.accent).row();
        if(node.content.description != null) body.add(node.content.description).wrap().color(arc.graphics.Color.lightGray).row();
        if(node.parent instanceof IndustrialNode parent){
            body.add("@industrial.research.parent").color(Pal.accent).row();
            if(!free && !IndustrialResearch.unlocked(parent.content, node.planet)) body.add("@industrial.research.parent.missing").wrap().color(Pal.remove).row();
            body.button(parent.content.localizedName, () -> open(parent)).minHeight(48).row();
        }
        for(var objective : node.objectives){
            body.table(row -> {
                row.image(objective.complete() ? Icon.ok : Icon.cancel).size(20).padRight(8).color(objective.complete() ? Pal.accent : Pal.remove);
                row.add(objective.display()).growX().wrap();
            }).row();
        }
        var items = IndustrialResearch.availableItems(node.planet);
        if(node.requirements.length > 0){
            body.add("@industrial.research.costs").color(Pal.accent).row();
            for(int i = 0; i < node.requirements.length; i++){
                var req = node.requirements[i];
                int paid = node.finishedRequirements[i].amount;
                body.table(row -> {
                    row.image(req.item.uiIcon).size(28).padRight(8);
                    row.add(req.item.localizedName).growX().wrap();
                    row.add(Core.bundle.format("industrial.research.paid", paid, req.amount, items.get(req.item))).right();
                }).row();
            }
        }
        if(node.children.any()){
            body.add("@industrial.research.children").color(Pal.accent).row();
            for(var child : node.children){
                if(child instanceof IndustrialNode next) body.button(next.content.localizedName, () -> open(next)).minHeight(48).row();
            }
        }
        cont.pane(body).grow().scrollX(false);
        buttons.clearChildren();
        buttons.defaults().size(Math.min(200f, width / 2f), 56f);
        buttons.button("@back", Icon.left, this::hide);
        buttons.button("@industrial.research.fund", Icon.add, () -> {
            IndustrialResearch.spend(node);
            changed.run();
            if(control != null && state.isGame() && state.getPlanet() == node.planet && control.saves.getCurrent() != null) control.saves.getCurrent().save();
            Core.settings.forceSave();
            rebuild();
        }).name("industrial-research-pay").disabled(b -> !canPay());
    }
}
