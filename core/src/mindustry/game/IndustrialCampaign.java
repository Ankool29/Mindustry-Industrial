package mindustry.game;

import arc.util.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Industrial campaign boundaries: sectors and resources never alias a classic planet. */
public final class IndustrialCampaign{
    private IndustrialCampaign(){}

    public static boolean isPlanet(@Nullable Planet planet){
        return planet instanceof IndustrialPlanet;
    }

    public static boolean active(){
        return state != null && state.rules.sector != null && isPlanet(state.rules.sector.planet);
    }

    public static void prepareInfo(Sector sector){
        if(!isPlanet(sector.planet)) return;
        sector.info.waves = sector.info.attack = sector.info.hasSpawns = false;
        sector.info.winWave = 0;
    }

    /** Explore the frontier from any established base, without a military/core-tier gate. */
    public static boolean canExplore(Sector sector){
        return isPlanet(sector.planet) && (sector.id == sector.planet.startSector || sector.hasBase() || sector.near().contains(Sector::hasBase));
    }

    public static @Nullable Sector findLauncher(Sector sector, @Nullable Sector preferred){
        if(preferred != null && preferred.planet == sector.planet && preferred.hasBase() && preferred.isNear(sector)) return preferred;
        return sector.near().find(Sector::hasBase);
    }

    /** First landing uses the normal finite starting stock, never a second free resupply. */
    public static boolean supplyStarter(){
        return active() && (state.rules.sector.info.origin == null || state.rules.sector.info.origin == state.rules.sector);
    }
}
