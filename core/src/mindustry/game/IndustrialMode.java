package mindustry.game;

import arc.struct.*;
import mindustry.content.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/**
 * Policy for peaceful custom games. Keep future industrial progression and deposit
 * policies here, separate from the shared machines and the campaign generators.
 */
public final class IndustrialMode{
    private IndustrialMode(){}

    /** Apply after map overrides, including content restored when loading a map. */
    public static void apply(Rules rules){
        rules.industrial = true;
        rules.waves = false;
        rules.waveTimer = false;
        rules.waveSending = false;
        rules.attackMode = false;
        rules.pvp = false;
        rules.editor = false;
        rules.winWave = 0;
        rules.canGameOver = false;
        rules.infiniteResources = false;
        rules.instantBuild = false;
        rules.buildCostMultiplier = 1f;
        rules.buildSpeedMultiplier = 1f;
        rules.unitCostMultiplier = 1f;
        rules.allowEditRules = false;
        rules.allowEditWorldProcessors = false;
        rules.disableWorldProcessors = true;
        rules.mission = null;
        rules.spawns = new Seq<>();
        rules.objectives = new MapObjectives();
        rules.objectiveFlags.clear();

        // Reset only existing team overrides; do not populate all 256 teams in saves.
        for(var team : rules.teams.values){
            if(team == null) continue;
            team.cheat = false;
            team.fillItems = false;
            team.infiniteResources = false;
            team.buildAi = false;
            team.rtsAi = false;
            team.prebuildAi = false;
            team.buildSpeedMultiplier = 1f;
            team.unitCostMultiplier = 1f;
        }
    }

    /** Clean the loaded world, never the map file or an unrelated saved game. */
    public static void prepareWorld(){
        if(!state.rules.industrial || state.isCampaign() || net.client()) return;

        apply(state.rules);
        for(var tile : world.tiles){
            if(tile.overlay() == Blocks.spawn) tile.clearOverlay();
            if(tile.build != null && tile.team() != state.rules.defaultTeam && tile.team() != Team.derelict){
                tile.setBlock(Blocks.air);
            }
        }
        removeHostileUnits();
        Groups.bullet.each(b -> b.absorb());
        state.enemies = 0;
    }

    /** Also reject delayed enemy spawns before entities can act. No per-tick tile scan. */
    public static void removeHostileUnits(){
        if(!state.rules.industrial || state.isCampaign() || net.client()) return;
        Groups.unit.each(u -> {
            if(u.team != state.rules.defaultTeam) u.remove();
        });
    }
}
