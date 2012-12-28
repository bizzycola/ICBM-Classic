package universalelectricity.prefab.multiblock;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import universalelectricity.core.vector.Vector3;
import universalelectricity.prefab.network.IPacketReceiver;
import universalelectricity.prefab.network.PacketManager;

import com.google.common.io.ByteArrayDataInput;

/**
 * This is a multiblock to be used for blocks that are bigger than one block.
 * 
 * @author Calclavia
 * 
 */
public class TileEntityMulti extends TileEntity implements IPacketReceiver
{
	// The the position of the main block
	public Vector3 mainBlockPosition;

	public void setMainBlock(Vector3 mainBlock)
	{
		this.mainBlockPosition = mainBlock;

		if (!this.worldObj.isRemote)
		{
			PacketManager.sendPacketToClients(this.getDescriptionPacket());
		}
	}

	@Override
	public Packet getDescriptionPacket()
	{
		if (this.mainBlockPosition != null) { return PacketManager.getPacket("BasicComponents", this, this.mainBlockPosition.x, this.mainBlockPosition.y, this.mainBlockPosition.z); }
		return null;
	}

	public void onBlockRemoval()
	{
		if (mainBlockPosition != null)
		{
			TileEntity tileEntity = this.worldObj.getBlockTileEntity((int) mainBlockPosition.x, (int) mainBlockPosition.y, (int) mainBlockPosition.z);

			if (tileEntity != null && tileEntity instanceof IMultiBlock)
			{
				IMultiBlock mainBlock = (IMultiBlock) tileEntity;

				if (mainBlock != null)
				{
					mainBlock.onDestroy(this);
				}
			}
		}
	}

	public boolean onBlockActivated(World par1World, int x, int y, int z, EntityPlayer par5EntityPlayer)
	{
		if (mainBlockPosition != null)
		{
			TileEntity tileEntity = this.worldObj.getBlockTileEntity((int) mainBlockPosition.x, (int) mainBlockPosition.y, (int) mainBlockPosition.z);

			if (tileEntity != null)
			{
				if (tileEntity instanceof IMultiBlock) { return ((IMultiBlock) tileEntity).onActivated(par5EntityPlayer); }
			}
		}

		return false;
	}

	/**
	 * Reads a tile entity from NBT.
	 */
	@Override
	public void readFromNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.readFromNBT(par1NBTTagCompound);

		this.mainBlockPosition = Vector3.readFromNBT("mainBlockPosition", par1NBTTagCompound);
	}

	/**
	 * Writes a tile entity to NBT.
	 */
	@Override
	public void writeToNBT(NBTTagCompound par1NBTTagCompound)
	{
		super.writeToNBT(par1NBTTagCompound);

		this.mainBlockPosition.writeToNBT("mainBlockPosition", par1NBTTagCompound);
	}

	/**
	 * Determines if this TileEntity requires update calls.
	 * 
	 * @return True if you want updateEntity() to be called, false if not
	 */
	public boolean canUpdate()
	{
		return false;
	}

	@Override
	public void handlePacketData(INetworkManager network, int packetType, Packet250CustomPayload packet, EntityPlayer player, ByteArrayDataInput dataStream)
	{
		try
		{
			this.mainBlockPosition = new Vector3(dataStream.readDouble(), dataStream.readDouble(), dataStream.readDouble());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}