package thunder.hack.modules.client;

import thunder.hack.gui.clickui.ClickUI;
import thunder.hack.modules.Module;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.PositionSetting;
import thunder.hack.utility.render.Render2DEngine;


import java.awt.*;


public class ClickGui extends Module {

    private static ClickGui INSTANCE = new ClickGui();
    private Setting<colorModeEn> colorMode = new Setting("ColorMode", colorModeEn.Static);
    public final Setting<ColorSetting> hcolor1 = new Setting<>("MainColor", new ColorSetting(-6974059));
    public final Setting<ColorSetting> acolor = new Setting<>("MainColor2", new ColorSetting(-8365735));
    public final Setting<ColorSetting> plateColor = new Setting<>("PlateColor", new ColorSetting(-14474718));
    public final Setting<ColorSetting> catColor = new Setting<>("CategoryColor", new ColorSetting(-15395563));
    public Setting<Integer> colorSpeed = new Setting<Integer>("ColorSpeed", 18, 2, 54);
    public Setting<Boolean> showBinds = new Setting<>("ShowBinds", true);
    public Setting<Boolean> msaa = new Setting<>("MSAA", true);

/*
    я хотел, а потом опять забил
    private final Setting<PositionSetting> combatCat = new Setting<>("combatCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> miscCat = new Setting<>("miscCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> renderCat = new Setting<>("renderCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> movementCat = new Setting<>("movementCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> playerCat = new Setting<>("playerCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> clientCat = new Setting<>("clientCat", new PositionSetting(0.5f, 0.5f));
    private final Setting<PositionSetting> hudCat = new Setting<>("hudCat", new PositionSetting(0.5f, 0.5f));

 */




    public ClickGui() {
        super("ClickGui", "кликгуи", Module.Category.CLIENT);
        this.setInstance();
    }

    public static ClickGui getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ClickGui();
        }
        return INSTANCE;
    }


    public Color getColor(int count) {
        int index = count;
        switch (colorMode.getValue()) {
            case Sky -> {
                return Render2DEngine.skyRainbow(colorSpeed.getValue(), index);
            }
            case LightRainbow -> {
                return Render2DEngine.rainbow((int) colorSpeed.getValue(), index, .6f, 1, 1);
            }
            case Rainbow -> {
                return Render2DEngine.rainbow((int) colorSpeed.getValue(), index, 1f, 1, 1);
            }
            case Fade -> {
                return Render2DEngine.fade((int) colorSpeed.getValue(), index, hcolor1.getValue().getColorObject(), 1);
            }
            case DoubleColor -> {
                return Render2DEngine.interpolateColorsBackAndForth((int) colorSpeed.getValue(), index,
                        hcolor1.getValue().getColorObject(), new Color(0xFFFFFFFF), true);
            }
            case Analogous -> {
                Color analogous = Render2DEngine.getAnalogousColor(acolor.getValue().getColorObject());
                return Render2DEngine.interpolateColorsBackAndForth((int) colorSpeed.getValue(), index, hcolor1.getValue().getColorObject(), analogous, true);
            }
            default -> {
                return hcolor1.getValue().getColorObject();
            }
        }
    }


    @Override
    public void onEnable() {
        mc.setScreen(ClickUI.getClickGui());
    }


    private void setInstance() {
        INSTANCE = this;
    }

    @Override
    public void onTick() {
        if (!(ClickGui.mc.currentScreen instanceof ClickUI))
            disable();
    }

    public enum colorModeEn {
        Static,
        Sky,
        LightRainbow,
        Rainbow,
        Fade,
        DoubleColor,
        Analogous
    }

}

