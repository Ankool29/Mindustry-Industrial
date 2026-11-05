package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

/** Read-only catalogue, not a new research/progression system. */
public class IndustrialCatalogDialog extends BaseDialog{
    private final Table list = new Table();
    private String search = "";

    public IndustrialCatalogDialog(){
        super("@industrial.catalog");
        shouldPause = true;
        addCloseButton();
        cont.add("@industrial.catalog.info").width(480f).wrap().pad(8).row();
        TextField field = new TextField();
        field.setMessageText(Core.bundle.get("players.search"));
        field.changed(() -> {
            search = field.getText().toLowerCase(Locale.ROOT);
            rebuild();
        });
        cont.add(field).growX().pad(8).row();
        cont.pane(list).grow().scrollX(false);
        shown(this::rebuild);
        onResize(this::rebuild);
    }

    private boolean matches(UnlockableContent value){
        return IndustrialContent.industrial(value) && value.isOnPlanet(Planets.industrialSerpulo) &&
            (search.isEmpty() || value.localizedName.toLowerCase(Locale.ROOT).contains(search));
    }

    private void section(String title, Seq<? extends UnlockableContent> entries){
        if(entries.isEmpty()) return;
        list.add(title).color(Pal.accent).left().padTop(12).row();
        for(var value : entries){
            list.button(b -> {
                b.image(value.uiIcon).size(40).padRight(12);
                b.add(value.localizedName).left().growX().wrap();
            }, Styles.flatt, () -> ui.content.showIndustrial(value))
                .name("industrial-catalog-" + value.name).width(480f).minHeight(56f).pad(2).row();
        }
    }

    private void rebuild(){
        list.clear();
        for(var category : Category.all){
            section(Core.bundle.get("database-tag." + category.name()), content.blocks().select(b -> b.category == category && matches(b) &&
                (b.buildVisibility == BuildVisibility.shown || b.buildVisibility == BuildVisibility.fogOnly) &&
                b.supportsEnv(Planets.industrialSerpulo.defaultEnv)));
        }
        section(Core.bundle.get("database-category.item"), content.items().select(i -> !i.isHidden() && matches(i)));
        section(Core.bundle.get("database-category.liquid"), content.liquids().select(i -> !i.isHidden() && matches(i)));
        section(Core.bundle.get("database-category.unit"), content.units().select(i -> !i.isHidden() && matches(i)));
    }
}
