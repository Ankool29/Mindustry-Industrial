package mindustry.game;

import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/** Presentation policy only. Never changes content, recipes, unlocks or saved buildings. */
public final class IndustrialContent{
    private IndustrialContent(){}

    private static int catalogDepth;

    /** Synchronous UI rendering outside a running industrial sector; always restore the scope. */
    public static void catalog(Runnable display){
        catalogDepth++;
        try{
            display.run();
        }finally{
            catalogDepth--;
        }
    }

    private static boolean filtering(){
        return active() || catalogDepth > 0;
    }

    public static boolean active(){
        return state != null && state.rules.industrial && (!state.isCampaign() || IndustrialCampaign.active());
    }

    /** Resources remain available: ammunition materials also feed industry and power plants. */
    public static boolean industrial(UnlockableContent value){
        if(value instanceof UnitType unit){
            return unit.buildSpeed > 0f || (unit.mineTier >= 0 && unit.mineSpeed > 0f) || unit.payloadCapacity >= tilesize * tilesize;
        }
        if(!(value instanceof Block block)) return true;
        if(block instanceof Turret || block.category == Category.turret || block.category == Category.defense ||
            block instanceof ForceProjector || block instanceof BaseShield || block instanceof ShockMine || block instanceof ShockwaveTower) return false;
        if(block instanceof UnitFactory factory) return factory.plans.contains(p -> industrial(p.unit));
        if(block instanceof Reconstructor reconstructor) return reconstructor.upgrades.contains(p -> industrial(p[1]));
        if(block instanceof UnitAssembler || block instanceof UnitAssemblerModule) return false;
        if(block.category == Category.units){
            // Payload transport/production and maintenance also serve civilian infrastructure.
            return block instanceof PayloadBlock || block instanceof PayloadConveyor || block instanceof RepairTurret || block instanceof RepairTower ||
                block instanceof UnitCargoLoader || block instanceof UnitCargoUnloadPoint;
        }
        return true;
    }

    public static boolean visible(UnlockableContent value){
        return !filtering() || industrial(value);
    }

    public static boolean visible(Schematic schematic){
        return !filtering() || !schematic.tiles.contains(t -> !industrial(t.block) || (IndustrialCampaign.strict() && !IndustrialResearch.available(t.block, state.rules.sector.planet)));
    }

    public static boolean industrial(Stat stat){
        return stat != Stat.weapons && stat != Stat.ammo && stat != Stat.ammoCapacity && stat != Stat.ammoUse;
    }

    public static boolean visible(Stat stat){
        return !filtering() || industrial(stat);
    }
}
