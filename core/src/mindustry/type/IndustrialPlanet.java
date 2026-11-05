package mindustry.type;

import mindustry.game.*;

/** A separately saved peaceful campaign, using an existing planet generator. */
public class IndustrialPlanet extends Planet{
    public final boolean strictResearch;
    public IndustrialPlanet(String name, Planet parent, float radius, int sectorSize){
        this(name, parent, radius, sectorSize, false);
    }

    public IndustrialPlanet(String name, Planet parent, float radius, int sectorSize, boolean strictResearch){
        super(name, parent, radius, sectorSize);
        this.strictResearch = strictResearch;
        alwaysUnlocked = true;
        autoAssignPlanet = false;
        allowCampaignRules = false;
        allowLaunchSchematics = true;
        allowLaunchLoadout = true;
        enemyInfiniteItems = false;
    }

    @Override
    public void init(){
        super.init();
        peacefulRules();
        for(var sector : sectors){
            sector.generateEnemyBase = false;
            IndustrialCampaign.prepareInfo(sector);
        }
    }

    private void peacefulRules(){
        allowWaves = allowSectorInvasion = clearSectorOnLose = false;
        campaignRules.sectorInvasion = campaignRules.rtsAI = campaignRules.randomWaveAI = false;
        campaignRules.clearSectorOnLose = false;
    }

    @Override
    public void applyRules(Rules rules, boolean customGame){
        peacefulRules();
        super.applyRules(rules, customGame);
        IndustrialMode.apply(rules);
        if(rules.sector != null) IndustrialCampaign.prepareInfo(rules.sector);
    }
}
