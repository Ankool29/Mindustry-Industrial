// Local-only UI harness. Not included in the fork or Android APK.
Events.on(ClientLoadEvent, cons(function(){
    Timer.schedule(run(function(){
        try{
            Core.bundle = Packages.arc.util.I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Packages.java.util.Locale.FRENCH);
            Vars.ui.showInfo("UI TEST START");
            if(Core.scene.getDialog() != null) Core.scene.getDialog().hide();
            const dialog = new Packages.mindustry.ui.dialogs.MapPlayDialog();
            dialog.show(Vars.maps.defaultMaps().first());
            const button = dialog.find("mode-industrial");
            if(button == null || button.isDisabled()) throw "Industrial mode button unavailable";
            button.fireClick();
            Timer.schedule(run(function(){
                try{
                    if(!button.isChecked()) throw "Industrial mode did not become selected";
                    Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission3-ui-portrait.png"));
                    Log.info("INDUSTRIAL_UI_PORTRAIT_OK");
                    Core.graphics.setWindowSize(1200, 720);
                    Timer.schedule(run(function(){
                        try{
                            Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission3-ui-landscape.png"));
                            const play = dialog.find("play-custom-map");
                            if(play == null || play.isDisabled()) throw "Play button unavailable";
                            play.fireClick();
                            Timer.schedule(run(function(){
                                try{
                                    if(!Vars.state.isGame() || !Vars.state.rules.industrial || Vars.state.rules.waves || Vars.state.rules.infiniteResources) throw "Industrial game did not start correctly";
                                    Packages.arc.util.ScreenUtils.saveScreenshot(new Packages.arc.files.Fi("/workspace/build-evidence/mission3-ui-playing.png"));
                                    Log.info("INDUSTRIAL_UI_PLAY_OK");
                                    Core.app.exit();
                                }catch(error){ Log.err("INDUSTRIAL_UI_FAIL " + error); Core.app.exit(); }
                            }), 8);
                        }catch(error){ Log.err("INDUSTRIAL_UI_FAIL " + error); Core.app.exit(); }
                    }), 2);
                }catch(error){ Log.err("INDUSTRIAL_UI_FAIL " + error); Core.app.exit(); }
            }), 2);
        }catch(error){ Log.err("INDUSTRIAL_UI_FAIL " + error); Core.app.exit(); }
    }), 5);
}));
