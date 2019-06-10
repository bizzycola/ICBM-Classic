package icbm.classic.content.machines.launcher.screen;

import com.builtbroken.jlib.data.vector.IPos3D;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import icbm.classic.api.energy.IEnergyBufferProvider;
import icbm.classic.api.explosion.ILauncherController;
import icbm.classic.api.explosion.LauncherType;
import icbm.classic.api.tile.IRadioWaveSender;
import icbm.classic.config.ConfigLauncher;
import icbm.classic.content.machines.launcher.TileLauncherPrefab;
import icbm.classic.content.machines.launcher.base.TileLauncherBase;
import icbm.classic.lib.LanguageUtility;
import icbm.classic.lib.network.IPacket;
import icbm.classic.lib.network.IPacketIDReceiver;
import icbm.classic.lib.network.packet.PacketTile;
import icbm.classic.lib.transform.vector.Pos;
import icbm.classic.prefab.FakeRadioSender;
import icbm.classic.prefab.inventory.ExternalInventory;
import icbm.classic.prefab.inventory.IInventoryProvider;
import icbm.classic.prefab.tile.EnumTier;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * This tile entity is for the screen of the missile launcher
 *
 * @author Calclavia
 */
public class TileLauncherScreen extends TileLauncherPrefab implements IPacketIDReceiver, ILauncherController, IEnergyBufferProvider, IInventoryProvider<ExternalInventory>, IPeripheral, IPeripheralTile
{
    // The missile launcher base in which this
    // screen is connected with
    public TileLauncherBase launcherBase = null;

    /** Height to wait before missile curves */
    public short lockHeight = 3;

    public ExternalInventory inventory;

    public int launchDelay = 0;
    protected ArrayList<IComputerAccess> attachedComputers = new ArrayList<>();


    @Override
    public ExternalInventory getInventory()
    {
        if (inventory == null)
        {
            inventory = new ExternalInventory(this, 2); //TODO figure out what these 2 slots did
        }
        return inventory;
    }

    @Override
    public void update()
    {
        super.update();
        if (this.launcherBase == null || this.launcherBase.isInvalid())
        {
            this.launcherBase = null;
            for (EnumFacing rotation : EnumFacing.HORIZONTALS)
            {
                final Pos position = new Pos((IPos3D) this).add(rotation);
                final TileEntity tileEntity = position.getTileEntity(world);
                if (tileEntity != null)
                {
                    if (tileEntity instanceof TileLauncherBase)
                    {
                        this.launcherBase = (TileLauncherBase) tileEntity;
                        if (isServer())
                        {
                            setRotation(rotation.getOpposite());
                            updateClient = true;
                        }
                    }
                }
            }
        }
        if (isServer())
        {
            //Delay launch, basically acts as a reload time
            if (launchDelay > 0)
            {
                launchDelay--;
            }
            //Only launch if redstone
            else if (ticks % 10 == 0 && world.getRedstonePowerFromNeighbors(getPos()) > 0)
            {
                this.launch();
            }

            //Update packet TODO see if this is needed
            if (ticks % 3 == 0)
            {
                sendDescPacket();
            }
        }
    }

    @Override
    public PacketTile getDescPacket()
    {
        return new PacketTile("desc", 0, this).addData(getEnergy(), this.getFrequency(), this.lockHeight, this.getTarget().xi(), this.getTarget().yi(), this.getTarget().zi());
    }

    @Override
    public PacketTile getGUIPacket()
    {
        return getDescPacket();
    }

    @Override
    public void placeMissile(ItemStack itemStack)
    {
        if (this.launcherBase != null)
        {
            if (!this.launcherBase.isInvalid())
            {
                this.launcherBase.getInventory().setInventorySlotContents(0, itemStack);
            }
        }
    }

    @Override
    public boolean read(ByteBuf data, int id, EntityPlayer player, IPacket packet)
    {
        if (!super.read(data, id, player, packet))
        {
            switch (id)
            {
                case 0:
                {
                    if (isClient())
                    {
                        //this.tier = data.readInt();
                        setEnergy(data.readInt());
                        this.setFrequency(data.readInt());
                        this.lockHeight = data.readShort();
                        this.setTarget(new Pos(data.readInt(), data.readInt(), data.readInt()));
                        return true;
                    }
                }
                case 1:
                {
                    this.setFrequency(data.readInt());
                    return true;
                }
                case 2:
                {
                    this.setTarget(new Pos(data.readInt(), data.readInt(), data.readInt()));
                    return true;
                }
                case 3:
                {
                    this.lockHeight = (short) Math.max(Math.min(data.readShort(), Short.MAX_VALUE), 3);
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    // Checks if the missile is launchable
    @Override
    public boolean canLaunch()
    {
        if (this.launcherBase != null && this.launcherBase.getMissileStack() != null)
        {
            if (this.checkExtract())
            {
                return this.launcherBase.isInRange(this.getTarget());
            }
        }
        return false;
    }

    /** Calls the missile launcher base to launch it's missile towards a targeted location */
    @Override
    public void launch()
    {
        if (this.canLaunch() && this.launcherBase.launchMissile(this.getTarget(), this.lockHeight))
        {
            //Reset delay
            switch (getTier())
            {
                case ONE:
                    launchDelay = ConfigLauncher.LAUNCHER_DELAY_TIER1;
                    break;
                case TWO:
                    launchDelay = ConfigLauncher.LAUNCHER_DELAY_TIER2;
                    break;
                case THREE:
                    launchDelay = ConfigLauncher.LAUNCHER_DELAY_TIER3;
                    break;
            }

            //Remove energy
            this.extractEnergy();

            //Mark client for update
            updateClient = true;

            queueComputerCraftEvent("missileLaunched", null);
        }
    }

    /**
     * Gets the display status of the missile launcher
     *
     * @return The string to be displayed
     */
    @Override
    public String getStatus()
    {
        String color = "\u00a74";
        String status = LanguageUtility.getLocal("gui.misc.idle");

        if (this.launcherBase == null)
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusMissing");
        }
        else if (!checkExtract())
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusNoPower");
        }
        else if (this.launcherBase.getMissileStack().isEmpty())
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusEmpty");
        }
        else if (this.getTarget() == null)
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusInvalid");
        }
        else if (this.launcherBase.isTargetTooClose(this.getTarget()))
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusClose");
        }
        else if (this.launcherBase.isTargetTooFar(this.getTarget()))
        {
            status = LanguageUtility.getLocal("gui.launcherscreen.statusFar");
        }
        else
        {
            color = "\u00a72";
            status = LanguageUtility.getLocal("gui.launcherscreen.statusReady");
        }

        return color + status;
    }

    /** Reads a tile entity from NBT. */
    @Override
    public void readFromNBT(NBTTagCompound par1NBTTagCompound)
    {
        super.readFromNBT(par1NBTTagCompound);
        //this.tier = par1NBTTagCompound.getInteger("tier");
        this.lockHeight = par1NBTTagCompound.getShort("targetHeight");
    }

    /** Writes a tile entity to NBT. */
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound par1NBTTagCompound)
    {
        //par1NBTTagCompound.setInteger("tier", this.tier);
        par1NBTTagCompound.setShort("targetHeight", this.lockHeight);
        return super.writeToNBT(par1NBTTagCompound);
    }

    @Override
    public int getEnergyConsumption()
    {
        switch (this.getTier())
        {
            case ONE:
                return ConfigLauncher.LAUNCHER_POWER_USAGE_TIER1;
            case TWO:
                return ConfigLauncher.LAUNCHER_POWER_USAGE_TIER2;
        }
        return ConfigLauncher.LAUNCHER_POWER_USAGE_TIER3;
    }

    @Override
    public int getEnergyBufferSize()
    {
        switch (this.getTier())
        {
            case ONE:
                return ConfigLauncher.LAUNCHER_POWER_CAP_TIER1;
            case TWO:
                return ConfigLauncher.LAUNCHER_POWER_CAP_TIER2;
        }
        return ConfigLauncher.LAUNCHER_POWER_CAP_TIER3;
    }

    @Override
    public LauncherType getLauncherType()
    {
        return LauncherType.TRADITIONAL;
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player)
    {
        return new ContainerLaunchScreen(player, this);
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player)
    {
        return new GuiLauncherScreen(player, this);
    }

    @Override
    public void receiveRadioWave(float hz, IRadioWaveSender sender, String messageHeader, Object[] data)
    {
        if (isServer())
        {
            //Floor frequency as we do not care about sub ranges
            int frequency = (int) Math.floor(hz);
            //Only tier 3 (2 for tier value) can be remotely fired
            if (getTier() == EnumTier.THREE && frequency == getFrequency() && launcherBase != null)
            {
                //Laser detonator signal
                if (messageHeader.equals("activateLauncherWithTarget"))
                {
                    Pos pos = (Pos) data[0];
                    if (new Pos((IPos3D) this).distance(pos) < this.launcherBase.getRange())
                    {
                        setTarget(pos);
                        launch();
                        ((FakeRadioSender) sender).player.sendMessage(new TextComponentString("Firing missile at " + pos));
                    }
                }
                //Remote detonator signal
                else if (messageHeader.equals("activateLauncher"))
                {
                    ((FakeRadioSender) sender).player.sendMessage(new TextComponentString("Firing missile at " + getTarget()));
                    launch();
                }
            }
        }
    }


    /* ComputerCraft Integration */
    @Override
    public String[] getMethodNames()
    {
        return new String[] { "launch", "getTarget", "setTarget", "canLaunch", "getMissile", "getStatus" };
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments ) throws LuaException
    {
        switch(method)
        {
            case 0:
                this.launch();
            case 1:
                return new Object[]{ this.getTarget().x(), this.getTarget().y(), this.getTarget().z() };
            case 2:
                try {
                    if (arguments[0] != null && arguments[1] != null && arguments[2] != null)
                        this.setTarget(new Pos((Double)arguments[0], (Double)arguments[1], (Double)arguments[2]));
                    else
                        throw new LuaException("Must provide 3 doubles for x,y,z on setTarget.");

                }
                catch (Exception ex)
                {
                     throw new LuaException("Must provide 3 doubles for x,y,z on setTarget.");
                }

            case 3:
                return new Object[]{ this.canLaunch() };

            case 4:
                try
                {
                    return new Object[] {this.launcherBase.getMissileStack().getDisplayName()};
                }
                catch (Exception ex)
                {
                    return null;
                }

            case 5:
                return new Object[]{ this.getStatus() };

        }

        throw new LuaException("No valid ICBM Launcher method called.");
    }

    protected void queueComputerCraftEvent(String event, Object[] args)
    {
        for(IComputerAccess comp : attachedComputers)
            comp.queueEvent(event, args);
    }

    @Override
    public void attach( @Nonnull IComputerAccess computer )
    {
        if(!attachedComputers.contains(computer))
            attachedComputers.add(computer);
    }

    @Override
    public void detach( @Nonnull IComputerAccess computer )
    {
        if(attachedComputers.contains(computer))
            attachedComputers.remove(computer);
    }

    @Override
    public String getType()
    {
        EnumTier tier = getTier();
        String ts = "1";
        if(tier == EnumTier.TWO)
            ts = "2";
        else if(tier == EnumTier.THREE)
            ts = "3";

        return "ICBMLauncher_Tier" + ts;
    }

    @Override
    public IPeripheral getPeripheral( @Nonnull EnumFacing side )
    {
        return this;
    }

    @Override
    public boolean equals( @Nullable IPeripheral other )
    {
        if(other != null && other == this)
            return true;

        return false;
    }
}
