package me.baiyueyue.dont_do_it.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class GameBookItem extends Item {

    /** 客户端注入的屏幕打开器 */
    @FunctionalInterface
    public interface ScreenOpener {
        void openMainScreen();
    }
    @FunctionalInterface
    public interface SettingsOpener {
        void open(int wordTimer, int specialEventTimer, int hearts, int gameRange, String mode);
    }

    private static ScreenOpener screenOpener;
    private static SettingsOpener settingsOpener;

    public static void setScreenOpener(ScreenOpener opener) { screenOpener = opener; }
    public static void setSettingsOpener(SettingsOpener opener) { settingsOpener = opener; }

    public GameBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() && screenOpener != null) {
            screenOpener.openMainScreen();
        }
        return ActionResult.SUCCESS;
    }

    /** 由设置界面调用，通知客户端打开设置 */
    public static void requestOpenSettings(int wordTimer, int specialEventTimer, int hearts, int gameRange, String mode) {
        if (settingsOpener != null) {
            settingsOpener.open(wordTimer, specialEventTimer, hearts, gameRange, mode);
        }
    }
}
