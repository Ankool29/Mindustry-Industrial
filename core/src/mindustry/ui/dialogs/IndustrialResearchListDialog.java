package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.game.IndustrialResearch.*;
import mindustry.ui.*;

import java.util.*;

/** Alternate touch navigation for the same native research nodes, with no separate state. */
public class IndustrialResearchListDialog extends BaseDialog{
    private final IndustrialNode root;
    private final Runnable changed;
    private final Table list = new Table();
    private String search = "";

    public IndustrialResearchListDialog(IndustrialNode root, Runnable changed){
        super("@industrial.research.title");
        this.root = root;
        this.changed = changed;
        shouldPause = true;
        addCloseButton();
        TextField field = new TextField();
        field.setMessageText(Core.bundle.get("players.search"));
        field.changed(() -> { search = field.getText().toLowerCase(Locale.ROOT); rebuild(); });
        cont.add(field).growX().pad(8).row();
        cont.pane(list).grow().scrollX(false);
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    private void rebuild(){
        list.clear();
        Seq<IndustrialNode> nodes = new Seq<>();
        root.each(n -> { if(n instanceof IndustrialNode industrial) nodes.add(industrial); });
        nodes.sort((a, b) -> a.content.localizedName.compareToIgnoreCase(b.content.localizedName));
        float width = Math.min(450f, Core.graphics.getWidth() / Scl.scl(1f) - 48f);
        for(var node : nodes){
            if(!search.isEmpty() && !node.content.localizedName.toLowerCase(Locale.ROOT).contains(search)) continue;
            list.button(b -> {
                b.image(node.content.uiIcon).size(36).padRight(10);
                b.add(node.content.localizedName).growX().wrap();
            }, Styles.flatt, () -> new IndustrialResearchDetailsDialog(node, changed).show())
                .name("research-list-" + node.content.name).width(width).minHeight(56).pad(2).row();
        }
    }
}
