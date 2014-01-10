package icbm.sentry.gui;

import icbm.Reference;
import icbm.sentry.ICBMSentry;
import icbm.sentry.platform.TileEntityTurretPlatform;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dark.lib.prefab.invgui.GuiMachineContainer;

/** A base class for gun platforms
 * 
 * @author Darkguardsman */
@SideOnly(Side.CLIENT)
public abstract class GuiPlatformBase extends GuiMachineContainer
{
    public GuiPlatformBase(InventoryPlayer player, Container container, TileEntityTurretPlatform tileEntity)
    {
        super(ICBMSentry.instance, container, player, tileEntity);
        TEXTURE = new ResourceLocation(Reference.DOMAIN, Reference.GUI_PATH + "gui_platform_terminal.png");

    }
}